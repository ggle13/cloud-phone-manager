package com.example.cloudphone.data.repository

import com.example.cloudphone.data.model.CloudPhone
import com.example.cloudphone.data.network.ApiService
import com.example.cloudphone.data.network.NetworkModule
import com.example.cloudphone.data.network.RenameRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CloudPhoneRepository(private val api: ApiService) {

    suspend fun fetchPhones(): List<CloudPhone> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCloudPhoneList()
            parseCloudPhoneResponse(response)
        } catch (e: Exception) {
            // App API 失败则尝试 H5 API
            try {
                val h5Response = api.getCloudPhoneListH5()
                parseCloudPhoneResponse(h5Response)
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    private fun parseCloudPhoneResponse(response: com.example.cloudphone.data.model.ApiResponse<List<CloudPhone>>): List<CloudPhone> {
        return when {
            response.code == 200 && response.rows != null -> response.rows
            response.code == 200 && response.data != null -> response.data
            else -> emptyList()
        }
    }

    suspend fun renamePhone(instanceId: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.renameInstance(RenameRequest(instanceId, newName))
            response.code == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun startPhone(instanceId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.startInstance(instanceId)
            response.code == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun stopPhone(instanceId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.stopInstance(instanceId)
            response.code == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun rebootPhone(instanceId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.restartInstance(instanceId)
            response.code == 200
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun create(
            tokenProvider: () -> String,
            deviceIdProvider: () -> String = { "" }
        ): CloudPhoneRepository {
            return CloudPhoneRepository(
                NetworkModule.provideApiService(tokenProvider, deviceIdProvider)
            )
        }
    }
}
