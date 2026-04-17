package com.example.myapplication.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myapplication.presentation.admin.AdminDashboardScreen
import com.example.myapplication.presentation.admin.AdminDeckPreviewScreen
import com.example.myapplication.presentation.settings.SettingsScreen

@Composable
fun AdminNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.AdminDashboard.route,
        modifier = modifier
    ) {
        composable(route = Screen.AdminDashboard.route) {
            AdminDashboardScreen()
        }
        composable(route = Screen.AdminUsers.route) {
            com.example.myapplication.presentation.admin.AdminUsersScreen(
                onLogout = onLogout
            )
        }
        composable(route = Screen.AdminAiLogs.route) {
            com.example.myapplication.presentation.admin.AdminAiLogsScreen()
        }
        composable(route = Screen.AdminReports.route) {
            com.example.myapplication.presentation.admin.AdminReportsScreen(
                onNavigateToDeck = { deckId ->
                    navController.navigate(Screen.AdminDeckPreview.createRoute(deckId))
                }
            )
        }
        composable(route = Screen.AdminSettings.route) {
            SettingsScreen(onLogout = onLogout)
        }
        composable(
            route = Screen.AdminDeckPreview.route,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) {
            AdminDeckPreviewScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
