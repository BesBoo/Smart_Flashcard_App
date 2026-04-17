package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════
//  MEMOHOP — CALM STUDY-FRIENDLY COLOR SYSTEM
// ═══════════════════════════════════════════════════════
//
//  A clean, minimal palette designed for long learning sessions.
//  Navy primary + Mint/Teal surfaces.
//
//  Light: Dark navy text on white/mint surfaces
//  Dark:  Mint text on deep navy surfaces

// ── Light Mode Palette ──────────────────────────────
val LightPrimary          = Color(0xFF272343)  // Navy — buttons, nav, actions
val LightBackground       = Color(0xFFFFFFFF)  // White — app root
val LightSurface          = Color(0xFFE3F6F5)  // Mint — cards, inputs, containers
val LightSecondarySurface = Color(0xFFBAE8E8)  // Teal — selected, hover, tags
val LightTextOnPrimary    = Color(0xFFFFFFFF)  // White text on navy buttons
val LightTextPrimary      = Color(0xFF000000)  // Black text on light bg

// ── Dark Mode Palette ───────────────────────────────
val DarkPrimary           = Color(0xFFE3F6F5)  // Mint — accent, active icons
val DarkBackground        = Color(0xFF121629)  // Deep navy — app root
val DarkSurface           = Color(0xFF1A1F3A)  // Navy — cards, inputs, containers
val DarkSecondarySurface  = Color(0xFF2D3561)  // Slate — selected, hover, tags
val DarkTextOnPrimary     = Color(0xFF121629)  // Dark text on mint accent
val DarkTextPrimary       = Color(0xFFFFFFFF)  // White text on dark bg

// ── Derived Tones (for Material3 container hierarchy) ─
val LightSurfaceDim      = Color(0xFFD4EFEE)  // Slightly darker mint
val LightSurfaceBright   = Color(0xFFF5FFFE)  // Near-white mint tint
val DarkSurfaceDim       = Color(0xFF0E1120)  // Deeper than background
val DarkSurfaceBright    = Color(0xFF232845)  // Between surface and secondary

// ── Outline / Border ────────────────────────────────
val LightOutline         = Color(0xFF5E5C6F)  // Muted navy for borders
val LightOutlineVariant  = Color(0xFFC0CFD0)  // Light border
val DarkOutline          = Color(0xFF4A5080)  // Subtle border on dark
val DarkOutlineVariant   = Color(0xFF2D3561)  // Very subtle dark border

// ── On-Surface Variant (secondary text) ─────────────
val LightOnSurfaceVariant = Color(0xFF000000)  // Black (secondary text)
val DarkOnSurfaceVariant  = Color(0xFFFFFFFF)  // White (secondary text)

// ── Error ────────────────────────────────────────────
val Error40       = Color(0xFFBA1A1A)   // Light error
val Error80       = Color(0xFFFFB4AB)   // Dark error
val ErrorContainer = Color(0xFFFFDAD6)  // Light error container
val ErrorContainerDark = Color(0xFF93000A) // Dark error container

// ── Semantic (SM-2 Review Quality) ──────────────────
val QualityAgain  = Color(0xFFEF5350)   // "Học lại" — red
val QualityHard   = Color(0xFFFF9800)   // "Khó" — orange
val QualityGood   = Color(0xFF66BB6A)   // "Tốt" — green
val QualityEasy   = Color(0xFF42A5F5)   // "Dễ" — blue

// ── Streak / Gamification ───────────────────────────
val StreakFlame   = Color(0xFFFF6D00)
val StreakGold    = Color(0xFFFFD600)

// ── Gradient helpers ────────────────────────────────
val GradientPrimaryStart = Color(0xFF272343)
val GradientPrimaryEnd   = Color(0xFF2D3561)
val GradientDarkBgStart  = Color(0xFF121629)
val GradientDarkBgEnd    = Color(0xFF1A1F3A)

// ══════════════════════════════════════════════════════
//  BACKWARD-COMPAT ALIASES (used by LoginScreen, etc.)
// ══════════════════════════════════════════════════════
val DarkNavy        = DarkBackground           // #121629
val DarkBlue        = DarkSurface              // #1A1F3A
val DeepBlue        = DarkSecondarySurface     // #2D3561
val AccentPurple    = LightPrimary             // #272343
val AccentBlue      = Color(0xFF6BCECE)        // Teal accent
val GradientStart   = GradientPrimaryStart
val GradientEnd     = GradientPrimaryEnd
val TextWhite       = Color(0xFFFFFFFF)
val TextGrey        = Color(0xFFFFFFFF)         // White (dark mode text)
val TextLightBlue   = Color(0xFFBAE8E8)        // Teal highlight
val SurfaceDark     = DarkSurface              // #1A1F3A
val SurfaceCard     = DarkSecondarySurface     // #2D3561
val InputFieldBg    = DarkSurface              // #1A1F3A
val InputFieldBorder= DarkSecondarySurface     // #2D3561
val SuccessGreen    = QualityGood
val ErrorRed        = QualityAgain
val WarningOrange   = QualityHard