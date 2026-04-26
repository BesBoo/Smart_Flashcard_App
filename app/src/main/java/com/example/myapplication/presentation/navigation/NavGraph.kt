package com.example.myapplication.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myapplication.presentation.aigenerate.AiGenerateScreen
import com.example.myapplication.presentation.utilities.ChatScreen
import com.example.myapplication.presentation.utilities.FlashcardQuizScreen
import com.example.myapplication.presentation.utilities.SmartReviewScreen
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

private const val TAB_TRANSITION_DURATION_MS = 360
private val bottomTabRoutes = Screen.bottomNavItems.map { it.route }

private fun bottomTabIndex(route: String?): Int = bottomTabRoutes.indexOf(route)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int {
    val from = bottomTabIndex(initialState.destination.route)
    val to = bottomTabIndex(targetState.destination.route)
    return when {
        from == -1 || to == -1 || from == to -> 0
        to > from -> 1
        else -> -1
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnterTransition(): EnterTransition {
    val direction = tabDirection()
    if (direction == 0) return EnterTransition.None

    return slideInHorizontally(
        animationSpec = tween(TAB_TRANSITION_DURATION_MS),
        initialOffsetX = { width -> if (direction > 0) width else -width }
    ) + fadeIn(animationSpec = tween(TAB_TRANSITION_DURATION_MS))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExitTransition(): ExitTransition {
    val direction = tabDirection()
    if (direction == 0) return ExitTransition.None

    return slideOutHorizontally(
        animationSpec = tween(TAB_TRANSITION_DURATION_MS),
        targetOffsetX = { width -> if (direction > 0) -width else width }
    ) + fadeOut(animationSpec = tween(TAB_TRANSITION_DURATION_MS))
}

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
        composable(
            route = Screen.Home.route,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() },
            popEnterTransition = { tabEnterTransition() },
            popExitTransition = { tabExitTransition() }
        ) {
            HomeScreen(
                onStartStudyClick = {
                    navController.navigate(Screen.StudySession.createRoute("all"))
                },
                onDeckClick = { deckId ->
                    navController.navigate(Screen.DeckDetail.createRoute(deckId))
                }
            )
        }
        composable(
            route = Screen.Decks.route,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() },
            popEnterTransition = { tabEnterTransition() },
            popExitTransition = { tabExitTransition() }
        ) {
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
        composable(
            route = Screen.Utilities.route,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() },
            popEnterTransition = { tabEnterTransition() },
            popExitTransition = { tabExitTransition() }
        ) {
            UtilitiesHubScreen(
                onOpenAiChat = {
                    navController.navigate(Screen.AiChat.route)
                },
                onOpenSmartReview = {
                    navController.navigate(Screen.SmartReview.route)
                },
                onOpenFlashcardQuiz = {
                    navController.navigate(Screen.FlashcardQuiz.route)
                }
            )
        }
        composable(
            route = Screen.Stats.route,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() },
            popEnterTransition = { tabEnterTransition() },
            popExitTransition = { tabExitTransition() }
        ) {
            StatsScreen()
        }
        composable(
            route = Screen.Settings.route,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() },
            popEnterTransition = { tabEnterTransition() },
            popExitTransition = { tabExitTransition() }
        ) {
            SettingsScreen(onLogout = onLogout)
        }

        // ── Utilities Sub-screens ──
        composable(route = Screen.AiChat.route) {
            ChatScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(route = Screen.SmartReview.route) {
            SmartReviewScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(route = Screen.FlashcardQuiz.route) {
            FlashcardQuizScreen(
                onNavigateBack = { navController.navigateUp() }
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
