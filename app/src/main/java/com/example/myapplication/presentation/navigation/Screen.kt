package com.example.myapplication.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Memory
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    
    // Auth
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    // Main Bottom Navigation Tabs
    object Home : Screen("home", "Trang chủ", Icons.Default.Home)
    object Decks : Screen("decks", "Thư viện", Icons.Default.LibraryBooks)
    object Utilities : Screen("utilities", "Tiện ích", Icons.Default.Widgets)
    object Stats : Screen("stats", "Thống kê", Icons.Default.BarChart)
    object Settings : Screen("settings", "Cài đặt", Icons.Default.Settings)

    // Sub-screens under Utilities
    object AiChat : Screen("ai_chat")
    object SmartReview : Screen("smart_review")

    /** Admin drawer navigation items */
    object AdminDashboard : Screen("admin_dashboard", "Dashboard", Icons.Default.Dashboard)
    object AdminUsers : Screen("admin_users", "Người dùng", Icons.Default.People)
    object AdminAiLogs : Screen("admin_ai_logs", "Quản lý AI", Icons.Default.Memory)
    object AdminReports : Screen("admin_reports", "Báo cáo", Icons.Default.BugReport)
    object AdminSettings : Screen("admin_settings", "Cài đặt", Icons.Default.Settings)

    // Sub-screens
    object DeckDetail : Screen("deck_detail/{deckId}") {
        fun createRoute(deckId: String) = "deck_detail/$deckId"
    }
    
    object StudySession : Screen("study_session/{deckId}") {
        fun createRoute(deckId: String) = "study_session/$deckId"
    }

    object FlashcardEditor : Screen("flashcard_editor/{deckId}/{cardId}") {
        fun createRoute(deckId: String, cardId: String = "new") = "flashcard_editor/$deckId/$cardId"
    }

    object AiGenerate : Screen("ai_generate/{deckId}") {
        fun createRoute(deckId: String) = "ai_generate/$deckId"
    }

    object Quiz : Screen("quiz/{deckId}") {
        fun createRoute(deckId: String) = "quiz/$deckId"
    }

    object JoinDeck : Screen("join_deck")

    object AdminDeckPreview : Screen("admin_deck_preview/{deckId}") {
        fun createRoute(deckId: String) = "admin_deck_preview/$deckId"
    }
    
    companion object {
        val bottomNavItems = listOf(Home, Decks, Utilities, Stats, Settings)
        val adminDrawerItems = listOf(AdminDashboard, AdminUsers, AdminAiLogs, AdminReports, AdminSettings)
    }
}
