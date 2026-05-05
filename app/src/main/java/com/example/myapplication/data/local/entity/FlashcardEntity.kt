package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "deckId"]),
        Index(value = ["userId", "nextReviewDate"]),
        Index(value = ["userId", "updatedAt"]),
        Index(value = ["deckId"])
    ]
)
data class FlashcardEntity(
    @PrimaryKey
    val id: String,                    // UUID from server
    val userId: String,
    val deckId: String,

    // Card content
    val frontText: String,
    val backText: String,
    val exampleText: String? = null,
    val pronunciationIpa: String? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null,

    // SM-2 state (client-calculated)
    val repetition: Int = 0,
    val intervalDays: Int = 1,
    val easeFactor: Double = 2.5,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val failCount: Int = 0,
    val totalReviews: Int = 0,

    // Soft delete & timestamps
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
