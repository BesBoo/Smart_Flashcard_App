package com.example.myapplication.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = Screen.adminDrawerItems.find { it.route == currentRoute }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = cs.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    // Header
                    Spacer(Modifier.height(12.dp))
                    Text("Admin Panel", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                    Text("Quản trị hệ thống", fontSize = 13.sp, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(28.dp))

                    // Drawer Items
                    Screen.adminDrawerItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) cs.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    if (!isSelected) {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.AdminDashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp)
                        ) {
                            if (screen.icon != null) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) cs.primary else cs.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                            }
                            Text(
                                text = screen.title ?: "",
                                color = if (isSelected) cs.primary else cs.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = cs.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = currentScreen?.title ?: "Admin",
                            color = cs.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = cs.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = cs.background
                    )
                )
            }
        ) { innerPadding ->
            AdminNavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                onLogout = onLogout
            )
        }
    }
}
