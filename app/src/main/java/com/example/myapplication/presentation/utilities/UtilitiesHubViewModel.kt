package com.example.myapplication.presentation.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.UserRepository
import com.example.myapplication.domain.usecase.GetLearningStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UtilitiesUiState(
    val isLoading: Boolean = true,
    val dueCards: Int = 0,
    val totalCards: Int = 0,
    val currentStreak: Int = 0,
    val isBubbleEnabled: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class UtilitiesHubViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getLearningStatsUseCase: GetLearningStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UtilitiesUiState())
    val uiState: StateFlow<UtilitiesUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun refresh() {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userId = userRepository.getCurrentUserId()
                if (userId != null) {
                    val stats = getLearningStatsUseCase(userId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            dueCards = stats.dueCards,
                            totalCards = stats.totalCards,
                            currentStreak = stats.currentStreak,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.localizedMessage)
                }
            }
        }
    }

    fun toggleBubble(enabled: Boolean) {
        _uiState.update { it.copy(isBubbleEnabled = enabled) }
        // Phase 4: Will start/stop FloatingBubbleService here
    }
}
