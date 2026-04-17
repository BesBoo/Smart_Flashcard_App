package com.example.myapplication.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════
//  SHAPES — Consistent corner radius system
// ═══════════════════════════════════════════════════════

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),     // chips, badges
    small      = RoundedCornerShape(8.dp),     // buttons, text fields
    medium     = RoundedCornerShape(12.dp),    // cards, dialogs
    large      = RoundedCornerShape(16.dp),    // bottom sheets, FABs
    extraLarge = RoundedCornerShape(24.dp),    // modals, search bar
)

// ── Additional shape tokens for specific use ────────
val FlashcardShape = RoundedCornerShape(20.dp)  // Flashcard cards
val DeckCardShape  = RoundedCornerShape(16.dp)  // Deck grid cards
val BottomNavShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
val ChipShape      = RoundedCornerShape(50)     // Fully rounded pills
