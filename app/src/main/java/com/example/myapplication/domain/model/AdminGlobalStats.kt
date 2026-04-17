package com.example.myapplication.domain.model

/** Aggregate statistics for admin dashboard (from GET /api/admin/stats). */
data class AdminGlobalStats(
    val totalUsers: Int,
    val activeUsers: Int,
    val totalDecks: Int,
    val totalFlashcards: Int,
    val totalReviews: Int,
    val premiumUsers: Int
)
