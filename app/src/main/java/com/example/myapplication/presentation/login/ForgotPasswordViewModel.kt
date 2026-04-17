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

data class ForgotPasswordUiState(
    val email: String = "",
    val otpSent: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isResetComplete: Boolean = false
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun sendOtp(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập email") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            try {
                val msg = userRepository.forgotPassword(email)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        otpSent = true,
                        email = email,
                        successMessage = msg
                    )
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("Failed to connect") == true -> "Không thể kết nối máy chủ."
                    e.message?.contains("chưa được đăng ký") == true -> "Email này chưa được đăng ký. Vui lòng kiểm tra lại."
                    else -> e.localizedMessage ?: "Đã xảy ra lỗi"
                }
                _uiState.update { it.copy(isLoading = false, error = errorMsg) }
            }
        }
    }

    fun resetPassword(otp: String, newPassword: String, confirmPassword: String) {
        if (otp.length != 6) {
            _uiState.update { it.copy(error = "Mã OTP phải có 6 chữ số") }
            return
        }
        if (newPassword.length < 6) {
            _uiState.update { it.copy(error = "Mật khẩu phải có ít nhất 6 ký tự") }
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(error = "Mật khẩu xác nhận không khớp") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            try {
                val msg = userRepository.resetPassword(_uiState.value.email, otp, newPassword)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isResetComplete = true,
                        successMessage = msg
                    )
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("OTP") == true -> "Mã OTP không hợp lệ hoặc đã hết hạn."
                    e.message?.contains("400") == true -> "Mã OTP không hợp lệ hoặc đã hết hạn."
                    else -> e.localizedMessage ?: "Đã xảy ra lỗi"
                }
                _uiState.update { it.copy(isLoading = false, error = errorMsg) }
            }
        }
    }

    fun resetState() {
        _uiState.update { ForgotPasswordUiState() }
    }
}
