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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.presentation.utilities.ChatBubbleState
import com.example.myapplication.presentation.utilities.ChatScreen
import com.example.myapplication.presentation.utilities.FloatingChatBubble

@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
    chatBubbleState: ChatBubbleState? = null
) {
    val navController = rememberNavController()
    val isBubbleEnabled by (chatBubbleState?.isEnabled
        ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val isChatOpen by (chatBubbleState?.isChatOpen
        ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()

    // Check if we're on a sub-screen (not a bottom nav tab) — hide bubble there
    val currentRoute by navController.currentBackStackEntryFlow
        .collectAsState(initial = null)
    val isOnBottomNavScreen = currentRoute?.destination?.route in
            Screen.bottomNavItems.map { it.route }

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
                    onNavigateBack = { chatBubbleState?.closeChat() }
                )
            }
        }
    }
}
