package com.example.myapplication.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    /** When true, navigate to admin dashboard flow after login. */
    val openAdminFlow: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập email và mật khẩu", isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Call actual backend / database via repository
                val result = userRepository.login(email, password)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        openAdminFlow = result.role == UserRole.ADMIN
                    )
                }
            } catch (e: Exception) {
                // Return clear error message to user
                val errorMessage = if (e.message?.contains("Failed to connect") == true || e.message?.contains("timeout") == true) {
                    "Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối mạng."
                } else if (e.message?.contains("401") == true) {
                    "Email hoặc mật khẩu không chính xác."
                } else {
                    e.localizedMessage ?: "Đã xảy ra lỗi không xác định"
                }
                
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = userRepository.googleLogin(idToken)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        openAdminFlow = result.role == UserRole.ADMIN
                    )
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("Failed to connect") == true -> "Không thể kết nối máy chủ."
                    e.message?.contains("401") == true -> "Đăng nhập Google thất bại."
                    else -> e.localizedMessage ?: "Đã xảy ra lỗi"
                }
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    fun resetState() {
        _uiState.update { LoginUiState() }
    }
}
