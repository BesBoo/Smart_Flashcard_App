package com.example.myapplication.domain.model

/**
 * Domain model for a Deck — a named group of flashcards.
 * This is the clean domain representation, free of Room/Retrofit annotations.
 */
data class Deck(
    val id: String,
    val userId: String,
    val name: String,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val language: String = "vi",
    val cardCount: Int = 0,
    val dueCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // ── Sharing fields ──
    val isOwner: Boolean = true,
    val permission: String? = null,       // null (owner) / "read" / "edit"
    val ownerName: String? = null,        // display name of deck owner (for subscribers)
    val shareCode: String? = null,        // share code (only visible to owner)
    val isShared: Boolean = false,        // deck has active share
    val googleSheetUrl: String? = null    // linked Google Sheet URL
)
