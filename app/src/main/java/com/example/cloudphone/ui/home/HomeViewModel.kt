package com.example.cloudphone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cloudphone.data.model.CloudPhone
import com.example.cloudphone.data.repository.CloudPhoneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: CloudPhoneRepository) : ViewModel() {

    private val _phones = MutableStateFlow<List<CloudPhone>>(emptyList())
    val phones: StateFlow<List<CloudPhone>> = _phones

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _activeTokenRemark = MutableStateFlow("")
    val activeTokenRemark: StateFlow<String> = _activeTokenRemark

    private val _operationLoading = MutableStateFlow<String?>(null)
    val operationLoading: StateFlow<String?> = _operationLoading

    init {
        loadPhones()
    }

    fun loadPhones() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _isRefreshing.value = true

            try {
                val list = repository.fetchPhones()
                if (list.isEmpty() && _phones.value.isEmpty()) {
                    // 可能是 Token 无效
                    _error.value = "列表为空，请检查 Token 是否正确"
                }
                _phones.value = list
            } catch (e: Exception) {
                val msg = e.message ?: "加载失败"
                when {
                    msg.contains("401") || msg.contains("Unauthorized") ->
                        _error.value = "Token 无效，请检查或更换"
                    msg.contains("network", ignoreCase = true) ->
                        _error.value = "网络连接失败，请检查网络"
                    else ->
                        _error.value = "加载失败: ${msg}"
                }
            } finally {
                _loading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun renamePhone(instanceId: String, newName: String) {
        viewModelScope.launch {
            _operationLoading.value = "rename_$instanceId"
            try {
                val ok = repository.renamePhone(instanceId, newName)
                if (ok) {
                    _message.value = "改名成功"
                    loadPhones()
                } else {
                    _message.value = "改名失败"
                }
            } catch (e: Exception) {
                _message.value = "改名失败: ${e.message}"
            } finally {
                _operationLoading.value = null
            }
        }
    }

    fun startPhone(instanceId: Int) {
        viewModelScope.launch {
            _operationLoading.value = "start_$instanceId"
            try {
                val ok = repository.startPhone(instanceId)
                if (ok) {
                    _message.value = "开机指令已发送"
                    loadPhones()
                } else {
                    _message.value = "开机失败"
                }
            } catch (e: Exception) {
                _message.value = "开机失败: ${e.message}"
            } finally {
                _operationLoading.value = null
            }
        }
    }

    fun stopPhone(instanceId: Int) {
        viewModelScope.launch {
            _operationLoading.value = "stop_$instanceId"
            try {
                val ok = repository.stopPhone(instanceId)
                if (ok) {
                    _message.value = "关机指令已发送"
                    loadPhones()
                } else {
                    _message.value = "关机失败"
                }
            } catch (e: Exception) {
                _message.value = "关机失败: ${e.message}"
            } finally {
                _operationLoading.value = null
            }
        }
    }

    fun rebootPhone(instanceId: Int) {
        viewModelScope.launch {
            _operationLoading.value = "reboot_$instanceId"
            try {
                val ok = repository.rebootPhone(instanceId)
                if (ok) {
                    _message.value = "重启指令已发送"
                    loadPhones()
                } else {
                    _message.value = "重启失败"
                }
            } catch (e: Exception) {
                _message.value = "重启失败: ${e.message}"
            } finally {
                _operationLoading.value = null
            }
        }
    }

    fun clearMessage() {
        _message.value = ""
    }

    fun setActiveTokenRemark(remark: String) {
        _activeTokenRemark.value = remark
    }
}

class HomeViewModelFactory(private val repository: CloudPhoneRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repository) as T
    }
}
