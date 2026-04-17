package com.example.myapplication.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(email: String, displayName: String, password: String, confirm: String) {
        if (email.isBlank() || displayName.isBlank() || password.isBlank() || confirm.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập đầy đủ thông tin", isLoading = false) }
            return
        }
        
        if (password != confirm) {
            _uiState.update { it.copy(error = "Mật khẩu xác nhận không khớp", isLoading = false) }
            return
        }
        
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Mật khẩu phải có ít nhất 6 ký tự", isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                userRepository.register(email, password, displayName)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                val errorMessage = if (e.message?.contains("CONFLICT") == true || e.message?.contains("409") == true) {
                    "Email này đã được sử dụng."
                } else if (e.message?.contains("connect") == true || e.message?.contains("timeout") == true) {
                    "Không thể kết nối máy chủ."
                } else {
                    e.localizedMessage ?: "Đã xảy ra lỗi không xác định"
                }
                
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    fun resetState() {
        _uiState.update { RegisterUiState() }
    }
}
