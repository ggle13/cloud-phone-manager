package com.example.cloudphone.ui.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cloudphone.data.model.TokenEntry
import com.example.cloudphone.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TokenViewModel(private val tokenManager: TokenManager) : ViewModel() {
    private val _tokens = MutableStateFlow<List<TokenEntry>>(emptyList())
    val tokens: StateFlow<List<TokenEntry>> = _tokens

    init {
        loadTokens()
    }

    fun loadTokens() {
        viewModelScope.launch {
            tokenManager.getTokensFlow().collect { list ->
                _tokens.value = list
            }
        }
    }

    fun addToken(token: String, remark: String) {
        viewModelScope.launch {
            val ok = tokenManager.addToken(token, remark)
            if (ok) loadTokens()
        }
    }

    fun activateToken(tokenId: String) {
        viewModelScope.launch {
            tokenManager.setActiveToken(tokenId)
            loadTokens()
        }
    }

    fun deleteToken(tokenId: String) {
        viewModelScope.launch {
            tokenManager.deleteToken(tokenId)
            loadTokens()
        }
    }

    fun renameToken(tokenId: String, newRemark: String) {
        viewModelScope.launch {
            tokenManager.renameToken(tokenId, newRemark)
        }
    }
}

class TokenViewModelFactory(private val tokenManager: TokenManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TokenViewModel(tokenManager) as T
    }
}
