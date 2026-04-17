package com.example.myapplication.presentation.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myapplication.ui.theme.BottomNavShape
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    tabs: List<Screen> = Screen.bottomNavItems
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val cs = MaterialTheme.colorScheme

    val showBottomBar = tabs.any { it.route == currentDestination?.route }

    if (showBottomBar) {
        NavigationBar(
            modifier = modifier.clip(BottomNavShape),
            containerColor = cs.surfaceContainer,
            contentColor = cs.onSurfaceVariant,
            tonalElevation = 8.dp
        ) {
            tabs.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                NavigationBarItem(
                    icon = {
                        screen.icon?.let {
                            Icon(imageVector = it, contentDescription = screen.title)
                        }
                    },
                    label = {
                        screen.title?.let {
                            Text(
                                text = it,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = cs.primary,
                        selectedTextColor = cs.primary,
                        indicatorColor = cs.primary.copy(alpha = 0.15f),
                        unselectedIconColor = cs.onSurfaceVariant,
                        unselectedTextColor = cs.onSurfaceVariant
                    )
                )
            }
        }
    }
}
