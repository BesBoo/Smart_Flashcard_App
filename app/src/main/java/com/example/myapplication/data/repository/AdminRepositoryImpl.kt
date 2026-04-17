package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.api.AdminApi
import com.example.myapplication.data.remote.dto.AdminAiLogDto
import com.example.myapplication.data.remote.dto.AdminAiStatsResponse
import com.example.myapplication.data.remote.dto.AdminBanUserRequest
import com.example.myapplication.data.remote.dto.AdminChangeRoleRequest
import com.example.myapplication.data.remote.dto.AdminDeckPreviewResponse
import com.example.myapplication.data.remote.dto.AdminReportActionRequest
import com.example.myapplication.data.remote.dto.AdminReportDto
import com.example.myapplication.data.remote.dto.AdminReportStatsResponse
import com.example.myapplication.data.remote.dto.AdminUserDto
import com.example.myapplication.data.remote.dto.SubmitReportRequest
import com.example.myapplication.domain.model.AdminGlobalStats
import com.example.myapplication.domain.repository.AdminRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val adminApi: AdminApi
) : AdminRepository {

    override suspend fun fetchGlobalStats(): AdminGlobalStats {
        val s = adminApi.getAdminStats()
        return AdminGlobalStats(
            totalUsers = s.totalUsers,
            activeUsers = s.activeUsers,
            totalDecks = s.totalDecks,
            totalFlashcards = s.totalFlashcards,
            totalReviews = s.totalReviews,
            premiumUsers = s.premiumUsers
        )
    }

    override suspend fun fetchUsers(search: String?, page: Int): List<AdminUserDto> {
        return adminApi.getUsers(search = search, page = page).users
    }

    override suspend fun banUser(userId: String, ban: Boolean) {
        adminApi.banUser(userId, AdminBanUserRequest(ban))
    }

    override suspend fun changeRole(userId: String, newRole: String) {
        adminApi.changeRole(userId, AdminChangeRoleRequest(newRole))
    }

    override suspend fun fetchAiLogs(page: Int, status: String?, type: String?): List<AdminAiLogDto> {
        return adminApi.getAiLogs(page = page, status = status, type = type).logs
    }

    override suspend fun fetchAiStats(): AdminAiStatsResponse {
        return adminApi.getAiStats()
    }

    override suspend fun fetchReports(page: Int): List<AdminReportDto> {
        return adminApi.getReports(page = page).reports
    }

    override suspend fun fetchReportStats(): AdminReportStatsResponse {
        return adminApi.getReportStats()
    }

    override suspend fun handleReport(reportId: String, action: String) {
        adminApi.handleReport(reportId, AdminReportActionRequest(action))
    }

    override suspend fun submitReport(targetType: String, targetId: String, reason: String) {
        adminApi.submitReport(SubmitReportRequest(targetType, targetId, reason))
    }

    override suspend fun fetchDeckPreview(deckId: String): AdminDeckPreviewResponse {
        return adminApi.getDeckPreview(deckId)
    }
}
