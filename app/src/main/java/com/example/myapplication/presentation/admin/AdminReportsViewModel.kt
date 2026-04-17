package com.example.myapplication.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.AdminReportDto
import com.example.myapplication.data.remote.dto.AdminReportStatsResponse
import com.example.myapplication.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminReportsUiState(
    val isLoading: Boolean = true,
    val reports: List<AdminReportDto> = emptyList(),
    val stats: AdminReportStatsResponse? = null,
    val error: String? = null,
    val actionMessage: String? = null,
    val selectedFilter: String = "Tất cả"
)

@HiltViewModel
class AdminReportsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminReportsUiState())
    val uiState: StateFlow<AdminReportsUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() { load() }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val reports = adminRepository.fetchReports()
                val stats = adminRepository.fetchReportStats()
                _uiState.update { it.copy(isLoading = false, reports = reports, stats = stats) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Lỗi tải reports") }
            }
        }
    }

    fun handleReport(reportId: String, action: String) {
        viewModelScope.launch {
            try {
                adminRepository.handleReport(reportId, action)
                _uiState.update { it.copy(actionMessage = "Đã ${if (action == "approve") "duyệt" else "từ chối"} báo cáo") }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(actionMessage = "Lỗi: ${e.localizedMessage}") }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    /** Filtered reports based on selected filter tab */
    val filteredReports: List<AdminReportDto>
        get() {
            val all = _uiState.value.reports
            return when (_uiState.value.selectedFilter) {
                "Chờ xử lý" -> all.filter { it.status == "Pending" }
                "Đã duyệt" -> all.filter { it.status == "Approved" }
                "Từ chối" -> all.filter { it.status == "Rejected" }
                else -> all
            }
        }
}
