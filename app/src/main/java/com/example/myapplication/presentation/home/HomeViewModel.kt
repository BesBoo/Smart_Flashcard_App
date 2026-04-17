package com.example.myapplication.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.sync.SyncScheduler
import com.example.myapplication.domain.model.LearningStats
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
    val pendingChanges: Int = 0
)

enum class SyncResult { SUCCESS, FAILED, IN_PROGRESS }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getLearningStatsUseCase: GetLearningStatsUseCase,
    private val syncDataUseCase: SyncDataUseCase,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Observable sync status from WorkManager */
    val isSyncing: StateFlow<Boolean> = syncScheduler.isSyncing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadHomeData()
        // Schedule periodic background sync on app start
        syncScheduler.schedulePeriodicSync()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = userRepository.getCurrentUserId()
                if (userId != null) {
                    val user = userRepository.getCurrentUser()
                    val userName = user?.displayName ?: "Học viên"

                    val stats = try {
                        getLearningStatsUseCase(userId)
                    } catch (e: Exception) {
                        null
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userName = userName,
                            stats = stats
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

            val newTimestamp = syncDataUseCase(userId, "0")
            _uiState.update {
                it.copy(
                    lastSyncResult = if (newTimestamp != null) SyncResult.SUCCESS else SyncResult.FAILED
                )
            }

            // Refresh data after sync
            if (newTimestamp != null) {
                loadHomeData()
            }
        }
    }

    fun refresh() {
        loadHomeData()
    }
}
