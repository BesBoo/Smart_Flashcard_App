package com.example.myapplication.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.sync.SyncScheduler
import com.example.myapplication.domain.model.Deck
import com.example.myapplication.domain.model.LearningStats
import com.example.myapplication.domain.repository.DeckRepository
import com.example.myapplication.domain.repository.ReviewLogRepository
import com.example.myapplication.domain.repository.UserRepository
import com.example.myapplication.domain.usecase.GetLearningStatsUseCase
import com.example.myapplication.domain.usecase.SyncDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val stats: LearningStats? = null,
    val error: String? = null,
    val lastSyncResult: SyncResult? = null,
    val pendingChanges: Int = 0,
    // Recent decks with due counts
    val recentDecks: List<Deck> = emptyList()
)

enum class SyncResult { SUCCESS, FAILED, IN_PROGRESS }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getLearningStatsUseCase: GetLearningStatsUseCase,
    private val syncDataUseCase: SyncDataUseCase,
    private val syncScheduler: SyncScheduler,
    private val deckRepository: DeckRepository,
    private val reviewLogRepository: ReviewLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Observable sync status from WorkManager */
    val isSyncing: StateFlow<Boolean> = syncScheduler.isSyncing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadHomeData(syncFirst = true)
        // Schedule periodic background sync on app start
        syncScheduler.schedulePeriodicSync()
    }

    private fun loadHomeData(syncFirst: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = userRepository.getCurrentUserId()
                if (userId != null) {
                    val user = userRepository.getCurrentUser()
                    val userName = user?.displayName ?: "Học viên"

                    // Only sync from server on first load or manual sync
                    // On tab switch / return from study, use local Room data
                    // (which already has the latest SM2 from the study session)
                    if (syncFirst) {
                        try {
                            deckRepository.syncDecks(userId)
                            reviewLogRepository.syncReviewLogs(userId)
                        } catch (_: Exception) {
                            // Offline — use local data
                        }
                    }

                    val stats = try {
                        getLearningStatsUseCase(userId)
                    } catch (e: Exception) {
                        null
                    }

                    // Load recent decks (sorted by most cards due, then newest)
                    val decks = try {
                        deckRepository.getDecksByUser(userId)
                            .sortedByDescending { it.dueCount }
                            .take(5) // Show top 5 recent/due decks
                    } catch (_: Exception) {
                        emptyList()
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userName = userName,
                            stats = stats,
                            recentDecks = decks
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error") }
            }
        }
    }

    /** Manual sync triggered by user */
    fun triggerSync() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch
            _uiState.update { it.copy(lastSyncResult = SyncResult.IN_PROGRESS) }

            try {
                deckRepository.syncDecks(userId)
                reviewLogRepository.syncReviewLogs(userId)
                _uiState.update { it.copy(lastSyncResult = SyncResult.SUCCESS) }
            } catch (_: Exception) {
                _uiState.update { it.copy(lastSyncResult = SyncResult.FAILED) }
            }

            // Refresh data after sync (no need to sync again)
            loadHomeData(syncFirst = false)
        }
    }

    /** Refresh stats from local Room (no server sync — fast) */
    fun refresh() {
        loadHomeData(syncFirst = false)
    }
}
