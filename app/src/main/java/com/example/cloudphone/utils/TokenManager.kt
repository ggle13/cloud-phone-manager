package com.example.cloudphone.utils

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cloudphone.data.model.TokenEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cloudphone_prefs")

class TokenManager(private val context: Context) {

    private val gson = Gson()

    companion object {
        private val TOKENS_KEY = stringPreferencesKey("tokens")
        private val ACTIVE_ID_KEY = stringPreferencesKey("active_token_id")
        private const val DEVICE_ID_KEY = "device_id"
    }

    // ─── Token 列表 ───
    fun getTokensFlow(): Flow<List<TokenEntry>> = context.dataStore.data.map { prefs ->
        val json = prefs[TOKENS_KEY] ?: "[]"
        try {
            gson.fromJson<List<TokenEntry>>(json, object : TypeToken<List<TokenEntry>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveTokens(tokens: List<TokenEntry>) {
        context.dataStore.edit { prefs ->
            prefs[TOKENS_KEY] = gson.toJson(tokens)
        }
    }

    // ─── 当前激活的 Token ───
    fun getActiveTokenFlow(): Flow<String?> = context.dataStore.data.map { prefs ->
        val activeId = prefs[ACTIVE_ID_KEY] ?: return@map null
        val tokens: List<TokenEntry> = try {
            gson.fromJson(prefs[TOKENS_KEY] ?: "[]",
                object : TypeToken<List<TokenEntry>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        tokens.find { it.id == activeId }?.token
    }

    suspend fun setActiveToken(tokenId: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_ID_KEY] = tokenId
        }
    }

    // ─── 当前账号的 UserId（从 JWT 自动解析） ───
    fun getActiveUserIdFlow(): Flow<String?> = context.dataStore.data.map { prefs ->
        val activeId = prefs[ACTIVE_ID_KEY] ?: return@map null
        val tokens: List<TokenEntry> = try {
            gson.fromJson(prefs[TOKENS_KEY] ?: "[]",
                object : TypeToken<List<TokenEntry>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val token = tokens.find { it.id == activeId }?.token ?: return@map null
        parseUserIdFromJwt(token)
    }

    // ─── 从 JWT 解析 userId（无需手动填写） ───
    fun parseUserIdFromJwt(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val payload = gson.fromJson(payloadJson, Map::class.java)
            // JWT payload 里的 user_id 或 sub 字段
            (payload["user_id"] ?: payload["sub"] ?: payload["userId"])?.toString()
        } catch (e: Exception) {
            null
        }
    }

    // ─── 当前 DeviceId（生成一次，持久化） ───
    fun getActiveDeviceId(): String {
        val prefs = context.getSharedPreferences("cloudphone_prefs", Context.MODE_PRIVATE)
        var deviceId = prefs.getString(DEVICE_ID_KEY, null)
        if (deviceId == null) {
            deviceId = generateDeviceId()
            prefs.edit().putString(DEVICE_ID_KEY, deviceId).apply()
        }
        return deviceId
    }

    private fun generateDeviceId(): String {
        return java.util.UUID.randomUUID().toString()
    }

    // ─── 添加 Token（自动解析 userId） ───
    suspend fun addToken(token: String, remark: String = ""): Boolean {
        val userId = parseUserIdFromJwt(token) ?: return false
        val tokens = getTokensFlow().first().toMutableList()
        val id = "token_${System.currentTimeMillis()}"
        tokens.add(TokenEntry(
            id = id,
            token = token,
            userId = userId,
            deviceId = getActiveDeviceId(),
            remark = remark.ifEmpty { userId.takeLast(6) }
        ))
        saveTokens(tokens)
        setActiveToken(id)
        return true
    }

    suspend fun deleteToken(tokenId: String) {
        val tokens = getTokensFlow().first().toMutableList()
        tokens.removeAll { it.id == tokenId }
        saveTokens(tokens)
        // 如果删除的是当前账号，自动切换
        val activeId = context.dataStore.data.first()[ACTIVE_ID_KEY]
        if (activeId == tokenId && tokens.isNotEmpty()) {
            setActiveToken(tokens.first().id)
        }
    }

    suspend fun renameToken(tokenId: String, newRemark: String) {
        val tokens = getTokensFlow().first().toMutableList()
        val index = tokens.indexOfFirst { it.id == tokenId }
        if (index >= 0) {
            tokens[index] = tokens[index].copy(remark = newRemark)
            saveTokens(tokens)
        }
    }

    // ─── 获取当前账号信息 ───
    suspend fun getActiveTokenEntry(): TokenEntry? {
        val activeId = context.dataStore.data.first()[ACTIVE_ID_KEY] ?: return null
        return getTokensFlow().first().find { it.id == activeId }
    }

    suspend fun getActiveToken(): String? = getActiveTokenFlow().first()
    suspend fun getActiveUserId(): String? = getActiveUserIdFlow().first()
}
