package com.example.myapplication.domain.repository

import com.example.myapplication.data.remote.dto.AdminAiLogDto
import com.example.myapplication.data.remote.dto.AdminAiStatsResponse
import com.example.myapplication.data.remote.dto.AdminDeckPreviewResponse
import com.example.myapplication.data.remote.dto.AdminReportDto
import com.example.myapplication.data.remote.dto.AdminReportStatsResponse
import com.example.myapplication.data.remote.dto.AdminUserDto
import com.example.myapplication.domain.model.AdminGlobalStats

interface AdminRepository {
    suspend fun fetchGlobalStats(): AdminGlobalStats
    suspend fun fetchUsers(search: String? = null, page: Int = 1): List<AdminUserDto>
    suspend fun banUser(userId: String, ban: Boolean)
    suspend fun changeRole(userId: String, newRole: String)
    suspend fun fetchAiLogs(page: Int = 1, status: String? = null, type: String? = null): List<AdminAiLogDto>
    suspend fun fetchAiStats(): AdminAiStatsResponse
    suspend fun fetchReports(page: Int = 1): List<AdminReportDto>
    suspend fun fetchReportStats(): AdminReportStatsResponse
    suspend fun handleReport(reportId: String, action: String)
    suspend fun submitReport(targetType: String, targetId: String, reason: String)
    suspend fun fetchDeckPreview(deckId: String): AdminDeckPreviewResponse
}
