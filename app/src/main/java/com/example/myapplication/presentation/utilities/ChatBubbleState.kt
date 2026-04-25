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

    /** Whether the chat overlay is currently open (expanding from bubble). */
    private val _isChatOpen = MutableStateFlow(false)
    val isChatOpen: StateFlow<Boolean> = _isChatOpen.asStateFlow()

    /** Normalized bubble position (0..1) for animation transform origin. */
    var bubbleNormalizedX: Float = 1f
        private set
    var bubbleNormalizedY: Float = 0.7f
        private set

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
    }

    fun toggle() {
        _isEnabled.value = !_isEnabled.value
    }

    fun openChat() {
        _isChatOpen.value = true
    }

    fun closeChat() {
        _isChatOpen.value = false
    }

    /** Called by FloatingChatBubble to report its current position for animation origin. */
    fun updateBubblePosition(normalizedX: Float, normalizedY: Float) {
        bubbleNormalizedX = normalizedX
        bubbleNormalizedY = normalizedY
    }
}
