package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        enableEdgeToEdge()
        setContent {
            val isDark by ThemeManager.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDark) {
                com.example.myapplication.presentation.navigation.RootNavGraph()
            }
        }
    }
}