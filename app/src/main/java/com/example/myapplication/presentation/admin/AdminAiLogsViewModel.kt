package com.example.myapplication.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.AdminAiLogDto
import com.example.myapplication.data.remote.dto.AdminAiStatsResponse
import com.example.myapplication.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminAiLogsUiState(
    val isLoading: Boolean = true,
    val logs: List<AdminAiLogDto> = emptyList(),
    val stats: AdminAiStatsResponse? = null,
    val error: String? = null,
    val selectedFilter: String = "Tất cả" // "Tất cả", "Thành công", "Thất bại", "Rate Limited"
)

@HiltViewModel
class AdminAiLogsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminAiLogsUiState())
    val uiState: StateFlow<AdminAiLogsUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() { load() }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val statusFilter = when (_uiState.value.selectedFilter) {
                    "Thành công" -> "Success"
                    "Thất bại" -> "Failed"
                    "Rate Limited" -> "RateLimited"
                    else -> null
                }
                val logs = adminRepository.fetchAiLogs(status = statusFilter)
                val stats = adminRepository.fetchAiStats()
                _uiState.update { it.copy(isLoading = false, logs = logs, stats = stats) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Lỗi tải logs") }
            }
        }
    }
}
