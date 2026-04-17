package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.AdminAiLogListResponse
import com.example.myapplication.data.remote.dto.AdminAiStatsResponse
import com.example.myapplication.data.remote.dto.AdminBanUserRequest
import com.example.myapplication.data.remote.dto.AdminChangeRoleRequest
import com.example.myapplication.data.remote.dto.AdminDeckPreviewResponse
import com.example.myapplication.data.remote.dto.AdminReportActionRequest
import com.example.myapplication.data.remote.dto.AdminReportListResponse
import com.example.myapplication.data.remote.dto.AdminReportStatsResponse
import com.example.myapplication.data.remote.dto.AdminStatsResponse
import com.example.myapplication.data.remote.dto.AdminUserListResponse
import com.example.myapplication.data.remote.dto.SubmitReportRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {

    // Dashboard Stats
    @GET("api/admin/stats")
    suspend fun getAdminStats(): AdminStatsResponse

    // User Management
    @GET("api/admin/users")
    suspend fun getUsers(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): AdminUserListResponse

    @POST("api/admin/users/{id}/ban")
    suspend fun banUser(@Path("id") id: String, @Body request: AdminBanUserRequest)

    @POST("api/admin/users/{id}/role")
    suspend fun changeRole(@Path("id") id: String, @Body request: AdminChangeRoleRequest)

    // AI Usage Logs
    @GET("api/admin/ai-logs")
    suspend fun getAiLogs(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
        @Query("status") status: String? = null,
        @Query("type") type: String? = null
    ): AdminAiLogListResponse

    // AI Stats
    @GET("api/admin/ai-stats")
    suspend fun getAiStats(): AdminAiStatsResponse

    // Content Reports
    @GET("api/admin/reports")
    suspend fun getReports(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): AdminReportListResponse

    // Report Stats
    @GET("api/admin/report-stats")
    suspend fun getReportStats(): AdminReportStatsResponse

    @POST("api/admin/reports/{id}/action")
    suspend fun handleReport(@Path("id") id: String, @Body request: AdminReportActionRequest)

    // User report submission (any authenticated user)
    @POST("api/reports")
    suspend fun submitReport(@Body request: SubmitReportRequest)

    // Admin deck preview (bypasses ownership check)
    @GET("api/admin/decks/{id}/preview")
    suspend fun getDeckPreview(@Path("id") deckId: String): AdminDeckPreviewResponse
}

