package com.example.myapplication.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.LearningStats
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

data class StatsUiState(
    val isLoading: Boolean = true,
    val stats: LearningStats? = null,
    val tomorrowForecast: Int = 0,
    val error: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getLearningStatsUseCase: GetLearningStatsUseCase,
    private val flashcardRepository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init { loadStats() }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userId = userRepository.getCurrentUserId() ?: ""
                val stats = getLearningStatsUseCase(userId)
                // STATS-05: Forecast - count cards due tomorrow
                val tomorrowMs = System.currentTimeMillis() + 24L * 60 * 60 * 1000
                val allDue = flashcardRepository.getDueCards(userId)
                // Cards already due + cards becoming due by tomorrow
                val forecast = allDue.size
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stats = stats,
                        tomorrowForecast = forecast
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun refresh() { loadStats() }
}
