package com.example.myapplication.ui.theme

import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════
//  SPACING & ELEVATION TOKENS
// ═══════════════════════════════════════════════════════
//
//  Consistent spacing scale based on 4dp grid.
//  Use these throughout all screens for visual harmony.

object Spacing {
    val xs   = 4.dp      // Tight gaps
    val sm   = 8.dp      // Between related items
    val md   = 16.dp     // Section padding, card padding
    val lg   = 24.dp     // Between sections
    val xl   = 32.dp     // Screen-level padding
    val xxl  = 48.dp     // Large spacers
    val xxxl = 64.dp     // Hero sections
}

object Elevation {
    val none    = 0.dp
    val xs      = 1.dp    // Subtle lift
    val sm      = 2.dp    // Cards
    val md      = 4.dp    // Floating buttons
    val lg      = 8.dp    // Dialogs, bottom sheets
    val xl      = 12.dp   // Navigation bar
}

object IconSize {
    val sm  = 16.dp
    val md  = 24.dp
    val lg  = 32.dp
    val xl  = 48.dp
    val xxl = 64.dp
}
