package com.example.cloudphone.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cloudphone.data.repository.LoginRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val countdown: Int = 0,
    val countingDown: Boolean = false,
    val token: String? = null
)

class LoginViewModel(private val repository: LoginRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private var countdownJob: Job? = null

    fun sendCode(phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)
            repository.sendCode(phone).onSuccess {
                startCountdown()
                _uiState.value = _uiState.value.copy(error = "验证码已发送")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message ?: "发送失败")
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(countingDown = true, countdown = 60)
            while (_uiState.value.countdown > 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(countdown = _uiState.value.countdown - 1)
            }
            _uiState.value = _uiState.value.copy(countingDown = false)
        }
    }

    fun login(phone: String, code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.login(phone, code).onSuccess { token ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    isLoggedIn = true,
                    token = token
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "登录失败"
                )
            }
        }
    }
}

class LoginViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LoginViewModel(LoginRepository()) as T
    }
}
