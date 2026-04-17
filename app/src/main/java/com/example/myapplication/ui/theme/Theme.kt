package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════
//  MEMOHOP THEME — Calm, Study-Friendly
// ═══════════════════════════════════════════════════════
//
//  Navy + Mint/Teal palette.
//  Clean, minimal, optimized for long reading sessions.

private val DarkColorScheme = darkColorScheme(
    // Primary — accent for active icons, highlights, key actions
    primary = DarkPrimary,                    // #E3F6F5
    onPrimary = DarkTextOnPrimary,            // #121629
    primaryContainer = DarkSecondarySurface,  // #2D3561
    onPrimaryContainer = DarkTextPrimary,     // #E3F6F5

    // Secondary
    secondary = DarkSecondarySurface,         // #2D3561
    onSecondary = DarkTextPrimary,            // #FFFFFF
    secondaryContainer = DarkSurface,         // #1A1F3A
    onSecondaryContainer = DarkTextPrimary,   // #FFFFFF

    // Tertiary — warm accent for variety
    tertiary = Color(0xFFFFCC93),             // Warm amber
    onTertiary = Color(0xFF331200),
    tertiaryContainer = Color(0xFF5C2400),
    onTertiaryContainer = Color(0xFFFFFFFF),

    // Error
    error = Error80,
    onError = Color(0xFF690005),
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainer,

    // Background & Surface
    background = DarkBackground,              // #121629
    onBackground = DarkTextPrimary,           // #FFFFFF
    surface = DarkBackground,                 // #121629
    onSurface = DarkTextPrimary,              // #FFFFFF
    surfaceVariant = DarkSurface,             // #1A1F3A
    onSurfaceVariant = DarkOnSurfaceVariant,  // #FFFFFF
    inverseSurface = LightSurface,            // #E3F6F5
    inverseOnSurface = LightPrimary,          // #272343

    // Outlines
    outline = DarkOutline,                    // #4A5080
    outlineVariant = DarkOutlineVariant,      // #2D3561

    // Container hierarchy (lightest → darkest)
    surfaceContainerLowest = DarkSurfaceDim,  // #0E1120
    surfaceContainerLow = DarkBackground,     // #121629
    surfaceContainer = DarkSurface,           // #1A1F3A — cards, input fields
    surfaceContainerHigh = DarkSurfaceBright, // #232845
    surfaceContainerHighest = DarkSecondarySurface, // #2D3561 — selected items
    surfaceDim = DarkSurfaceDim,              // #0E1120
    surfaceBright = DarkSurfaceBright,        // #232845
    surfaceTint = DarkPrimary,                // #E3F6F5

    // Inverse
    inversePrimary = LightPrimary,            // #272343
)

private val LightColorScheme = lightColorScheme(
    // Primary — navy for buttons, nav, important actions
    primary = LightPrimary,                   // #272343
    onPrimary = LightTextOnPrimary,           // #FFFFFF
    primaryContainer = LightSecondarySurface, // #BAE8E8
    onPrimaryContainer = LightPrimary,        // #272343

    // Secondary
    secondary = LightSecondarySurface,        // #BAE8E8
    onSecondary = LightTextPrimary,           // #000000
    secondaryContainer = LightSurface,        // #E3F6F5
    onSecondaryContainer = LightTextPrimary,  // #000000

    // Tertiary — warm accent
    tertiary = Color(0xFFAD5000),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE0C0),
    onTertiaryContainer = Color(0xFF000000),

    // Error
    error = Error40,
    onError = Color(0xFFFFFFFF),
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFF410002),

    // Background & Surface
    background = LightBackground,             // #FFFFFF
    onBackground = LightTextPrimary,          // #000000
    surface = LightBackground,                // #FFFFFF
    onSurface = LightTextPrimary,             // #000000
    surfaceVariant = LightSurface,            // #E3F6F5
    onSurfaceVariant = LightOnSurfaceVariant, // #000000
    inverseSurface = LightPrimary,            // #272343
    inverseOnSurface = LightSurface,          // #E3F6F5

    // Outlines
    outline = LightOutline,                   // #5E5C6F
    outlineVariant = LightOutlineVariant,     // #C0CFD0

    // Container hierarchy
    surfaceContainerLowest = LightBackground, // #FFFFFF
    surfaceContainerLow = LightSurfaceBright, // #F5FFFE
    surfaceContainer = LightSurface,          // #E3F6F5 — cards, input fields
    surfaceContainerHigh = LightSurfaceDim,   // #D4EFEE
    surfaceContainerHighest = LightSecondarySurface, // #BAE8E8 — selected
    surfaceDim = LightSurfaceDim,             // #D4EFEE
    surfaceBright = LightBackground,          // #FFFFFF
    surfaceTint = LightPrimary,               // #272343

    // Inverse
    inversePrimary = DarkPrimary,             // #E3F6F5
)

@Composable
fun SmartFlashcardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Tint the status bar
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar color to match background
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

// Keep old name as alias for backwards compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = SmartFlashcardTheme(darkTheme = darkTheme, content = content)