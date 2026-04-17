package com.example.myapplication.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.AdminGlobalStats
import com.example.myapplication.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val stats: AdminGlobalStats? = null,
    val pendingReportsCount: Int = 0,
    val activeRate: Float = 0f,
    val error: String? = null
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val stats = adminRepository.fetchGlobalStats()
                val activeRate = if (stats.totalUsers > 0)
                    stats.activeUsers.toFloat() / stats.totalUsers else 0f

                // Fetch pending reports count
                val pendingCount = try {
                    adminRepository.fetchReports().count { it.status == "Pending" }
                } catch (_: Exception) { 0 }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stats = stats,
                        pendingReportsCount = pendingCount,
                        activeRate = activeRate,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.localizedMessage ?: "Không tải được dữ liệu")
                }
            }
        }
    }
}
