package com.example.myapplication.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.presentation.utilities.ChatBubbleState
import com.example.myapplication.presentation.utilities.FloatingChatBubble

@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
    chatBubbleState: ChatBubbleState? = null
) {
    val navController = rememberNavController()
    val isBubbleEnabled by (chatBubbleState?.isEnabled
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
            FloatingChatBubble(
                isVisible = isBubbleEnabled && isOnBottomNavScreen,
                onOpenChat = {
                    navController.navigate(Screen.AiChat.route)
                }
            )
        }
    }
}
