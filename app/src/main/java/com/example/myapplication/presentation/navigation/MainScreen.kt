package com.example.myapplication.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen(
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        MainNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            onLogout = onLogout
        )
    }
}
