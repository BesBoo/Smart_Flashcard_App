package com.example.myapplication.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.notification.ReminderScheduler
import com.example.myapplication.domain.model.User
import com.example.myapplication.domain.repository.UserRepository
import com.example.myapplication.presentation.utilities.ChatBubbleState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val loggedOut: Boolean = false,
    val newCardsPerDay: Int = 40,
    val reviewCardsPerDay: Int = 150,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val isBubbleEnabled: Boolean = false,
    val emailUpdateResult: String? = null,
    val emailUpdateError: String? = null,
    val isUpdatingEmail: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val reminderScheduler: ReminderScheduler,
    private val chatBubbleState: ChatBubbleState,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val prefs by lazy {
        context.getSharedPreferences("study_settings", Context.MODE_PRIVATE)
    }

    init {
        loadUser()
        observeBubble()
    }

    private fun observeBubble() {
        viewModelScope.launch {
            chatBubbleState.isEnabled.collect { enabled ->
                _uiState.update { it.copy(isBubbleEnabled = enabled) }
            }
        }
    }

    fun toggleBubble(enabled: Boolean) {
        chatBubbleState.setEnabled(enabled)
    }

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                val newCards = prefs.getInt("new_cards_per_day", 40)
                val reviewCards = prefs.getInt("review_cards_per_day", 150)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        newCardsPerDay = newCards,
                        reviewCardsPerDay = reviewCards,
                        reminderEnabled = reminderScheduler.isEnabled(),
                        reminderHour = reminderScheduler.getHour(),
                        reminderMinute = reminderScheduler.getMinute()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateNewCardsPerDay(value: Int) {
        val clamped = value.coerceIn(5, 1000)
        prefs.edit().putInt("new_cards_per_day", clamped).apply()
        _uiState.update { it.copy(newCardsPerDay = clamped) }
    }

    fun updateReviewCardsPerDay(value: Int) {
        val clamped = value.coerceIn(10, 5000)
        prefs.edit().putInt("review_cards_per_day", clamped).apply()
        _uiState.update { it.copy(reviewCardsPerDay = clamped) }
    }

    fun toggleReminder(enabled: Boolean) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
        if (enabled) {
            val state = _uiState.value
            reminderScheduler.schedule(state.reminderHour, state.reminderMinute)
        } else {
            reminderScheduler.cancel()
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
        if (_uiState.value.reminderEnabled) {
            reminderScheduler.schedule(hour, minute)
        }
    }

    fun updateEmail(newEmail: String) {
        if (newEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            _uiState.update { it.copy(emailUpdateError = "Email không hợp lệ") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingEmail = true, emailUpdateError = null, emailUpdateResult = null) }
            try {
                val msg = userRepository.updateEmail(newEmail)
                _uiState.update {
                    it.copy(
                        isUpdatingEmail = false,
                        emailUpdateResult = "Email đã được cập nhật. Đang chuyển về đăng nhập..."
                    )
                }
                kotlinx.coroutines.delay(1500)
                userRepository.logout()
                _uiState.update { it.copy(loggedOut = true) }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("đã được sử dụng") == true -> "Email này đã được sử dụng bởi tài khoản khác."
                    e.message?.contains("Failed to connect") == true -> "Không thể kết nối máy chủ."
                    else -> e.localizedMessage ?: "Đã xảy ra lỗi"
                }
                _uiState.update { it.copy(isUpdatingEmail = false, emailUpdateError = errorMsg) }
            }
        }
    }

    fun clearEmailResult() {
        _uiState.update { it.copy(emailUpdateResult = null, emailUpdateError = null) }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _uiState.update { it.copy(loggedOut = true) }
        }
    }
}
