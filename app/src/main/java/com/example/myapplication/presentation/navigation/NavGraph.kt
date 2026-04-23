package com.example.myapplication.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myapplication.presentation.aigenerate.AiGenerateScreen
import com.example.myapplication.presentation.aitutor.AiTutorScreen
import com.example.myapplication.presentation.utilities.UtilitiesHubScreen
import com.example.myapplication.presentation.deckdetail.DeckDetailScreen
import com.example.myapplication.presentation.decks.DecksScreen
import com.example.myapplication.presentation.flashcardeditor.FlashcardEditorScreen
import com.example.myapplication.presentation.home.HomeScreen
import com.example.myapplication.presentation.quiz.QuizScreen
import com.example.myapplication.presentation.settings.SettingsScreen
import com.example.myapplication.presentation.share.JoinDeckScreen
import com.example.myapplication.presentation.stats.StatsScreen
import com.example.myapplication.presentation.study.StudySessionScreen

@Composable
fun MainNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // ── Bottom Navigation Tabs ──
        composable(route = Screen.Home.route) {
            HomeScreen(
                onStartStudyClick = {
                    navController.navigate(Screen.StudySession.createRoute("all"))
                }
            )
        }
        composable(route = Screen.Decks.route) {
            DecksScreen(
                onDeckClick = { deckId ->
                    navController.navigate(Screen.DeckDetail.createRoute(deckId))
                },
                onCreateDeckClick = { },
                onJoinDeckClick = {
                    navController.navigate(Screen.JoinDeck.route)
                }
            )
        }
        composable(route = Screen.Utilities.route) {
            UtilitiesHubScreen(
                onOpenAiChat = {
                    navController.navigate(Screen.AiChat.route)
                },
                onOpenSmartReview = {
                    navController.navigate(Screen.SmartReview.route)
                }
            )
        }
        composable(route = Screen.Stats.route) {
            StatsScreen()
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(onLogout = onLogout)
        }

        // ── Utilities Sub-screens ──
        composable(route = Screen.AiChat.route) {
            AiTutorScreen()  // Reuses existing AI Tutor screen (enhanced in Phase 2)
        }
        composable(route = Screen.SmartReview.route) {
            // Phase 3: SmartReviewScreen will replace this placeholder
            androidx.compose.material3.Text(
                text = "Smart Review — Coming Soon",
                modifier = Modifier.padding(32.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
        }

        // ── Sub-screens ──
        composable(
            route = Screen.DeckDetail.route,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) {
            DeckDetailScreen(
                onNavigateBack = { navController.navigateUp() },
                onStartStudy = { deckId ->
                    navController.navigate(Screen.StudySession.createRoute(deckId))
                },
                onAddCard = { deckId ->
                    navController.navigate(Screen.FlashcardEditor.createRoute(deckId, "new"))
                },
                onEditCard = { deckId, cardId ->
                    navController.navigate(Screen.FlashcardEditor.createRoute(deckId, cardId))
                },
                onAiGenerate = { deckId ->
                    navController.navigate(Screen.AiGenerate.createRoute(deckId))
                },
                onQuiz = { deckId ->
                    navController.navigate(Screen.Quiz.createRoute(deckId))
                }
            )
        }
        composable(
            route = Screen.StudySession.route,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) {
            StudySessionScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(
            route = Screen.FlashcardEditor.route,
            arguments = listOf(
                navArgument("deckId") { type = NavType.StringType },
                navArgument("cardId") { type = NavType.StringType; defaultValue = "new" }
            )
        ) {
            FlashcardEditorScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(
            route = Screen.AiGenerate.route,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) {
            AiGenerateScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(
            route = Screen.Quiz.route,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) {
            QuizScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(route = Screen.JoinDeck.route) {
            JoinDeckScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
