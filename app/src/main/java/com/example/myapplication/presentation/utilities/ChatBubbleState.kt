package com.example.myapplication.presentation.utilities

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton state holder for the floating chat bubble.
 * Shared between UtilitiesHubViewModel (toggle) and MainScreen (display).
 */
@Singleton
class ChatBubbleState @Inject constructor() {
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
    }

    fun toggle() {
        _isEnabled.value = !_isEnabled.value
    }
}
