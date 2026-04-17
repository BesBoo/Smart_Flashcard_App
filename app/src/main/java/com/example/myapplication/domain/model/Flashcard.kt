package com.example.myapplication.domain.model

/**
 * Domain model for a Flashcard — the core learning unit.
 *
 * Contains card content (front, back, example, media)
 * and embeds SM-2 scheduling state via [SM2Data].
 */
data class Flashcard(
    val id: String,
    val userId: String,
    val deckId: String,

    // Card content
    val frontText: String,
    val backText: String,
    val exampleText: String? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null,

    // SM-2 state (encapsulated)
    val sm2: SM2Data = SM2Data(),

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Convenience: true if this card is due for review */
    val isDue: Boolean get() = sm2.isDue

    /** Convenience: true if this card has never been reviewed */
    val isNew: Boolean get() = sm2.isNew

    /** Convenience: true if this card is struggling (failCount >= 3) */
    val isStruggling: Boolean get() = sm2.isStruggling
}
