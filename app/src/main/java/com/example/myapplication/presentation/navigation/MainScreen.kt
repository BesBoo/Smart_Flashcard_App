package com.example.myapplication.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.presentation.catpet.CatHungerManager
import com.example.myapplication.presentation.catpet.CatPetController
import com.example.myapplication.presentation.catpet.CatPetOverlay
import com.example.myapplication.presentation.catpet.RewardType
import com.example.myapplication.presentation.utilities.ChatBubbleState
import com.example.myapplication.presentation.utilities.ChatScreen
import com.example.myapplication.presentation.utilities.FloatingChatBubble

@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
    chatBubbleState: ChatBubbleState? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val isBubbleEnabled by (chatBubbleState?.isEnabled
        ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val isChatOpen by (chatBubbleState?.isChatOpen
        ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()

    // Check if we're on a sub-screen (not a bottom nav tab) — hide bubble there
    val currentRoute by navController.currentBackStackEntryFlow
        .collectAsState(initial = null)
    val isOnBottomNavScreen = currentRoute?.destination?.route in
            Screen.bottomNavItems.map { it.route }

    // ── Shared pet state ─────────────────────────────────
    val hungerManager = remember { CatHungerManager(context) }
    val catController = remember { CatPetController() }

    // ── Initialize hunger + check rewards on first composition ──
    LaunchedEffect(Unit) {
        hungerManager.init()
        catController.setHungerBand(hungerManager.getHungerBand())

        // Check for pending fish rewards (from a previous study session)
        val pendingFish = hungerManager.takePendingFish()
        if (pendingFish > 0) {
            catController.feedFish(
                count = pendingFish,
                celebrate = pendingFish >= 3
            )
        }
    }

    // ── Show bubble when returning to bottom nav tabs ────
    LaunchedEffect(isOnBottomNavScreen) {
        if (isOnBottomNavScreen && hungerManager.shouldShowBubble()) {
            // Estimate dueCards from hunger context (simplified)
            val dueCards = 0 // Will show generic message; exact count requires ViewModel
            val message = hungerManager.getBubbleMessage(dueCards)
            catController.showBubble(message)
            hungerManager.markBubbleShown()
        }
    }

    // ── Update hunger band periodically ──────────────────
    LaunchedEffect(isOnBottomNavScreen) {
        if (isOnBottomNavScreen) {
            hungerManager.init() // Recalculate hunger from time
            catController.setHungerBand(hungerManager.getHungerBand())

            // Check pending fish again (user might have just finished a session)
            val pendingFish = hungerManager.takePendingFish()
            if (pendingFish > 0) {
                catController.feedFish(
                    count = pendingFish,
                    celebrate = pendingFish >= 3
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            MainNavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                onLogout = onLogout
            )

            // Cat pet overlay — walks on top of the bottom navbar
            // Disable tap during study/review to avoid blocking answer buttons
            val currentRouteStr = currentRoute?.destination?.route ?: ""
            val isOnStudyScreen = currentRouteStr.startsWith("study_session") ||
                    currentRouteStr == "smart_review" ||
                    currentRouteStr == "flashcard_quiz" ||
                    currentRouteStr.startsWith("quiz")

            CatPetOverlay(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding() * 0.80f),
                controller = catController,
                isTapEnabled = !isOnStudyScreen
            )

            // Floating Chat Bubble overlay (above all screens)
            // Hide bubble when chat overlay is open
            if (chatBubbleState != null) {
                FloatingChatBubble(
                    isVisible = isBubbleEnabled && isOnBottomNavScreen && !isChatOpen,
                    chatBubbleState = chatBubbleState
                )
            }

            // ── Expanding Chat Overlay ──
            // Opens from bubble position with scale + fade animation
            AnimatedVisibility(
                visible = isChatOpen,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = 1f,
                        stiffness = 200f
                    ),
                    transformOrigin = TransformOrigin(
                        chatBubbleState?.bubbleNormalizedX ?: 1f,
                        chatBubbleState?.bubbleNormalizedY ?: 0.7f
                    )
                ) + fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(400)
                ),
                exit = scaleOut(
                    animationSpec = spring(
                        dampingRatio = 1f,
                        stiffness = 200f
                    ),
                    transformOrigin = TransformOrigin(
                        chatBubbleState?.bubbleNormalizedX ?: 1f,
                        chatBubbleState?.bubbleNormalizedY ?: 0.7f
                    )
                ) + fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            ) {
                ChatScreen(
                    modifier = Modifier.padding(innerPadding),
                    onNavigateBack = { chatBubbleState?.closeChat() }
                )
            }
        }
    }
}
