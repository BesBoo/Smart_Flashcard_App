package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FlashcardEntity::class,
            parentColumns = ["id"],
            childColumns = ["flashcardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["flashcardId"]),
        Index(value = ["userId", "reviewedAt"])
    ]
)
data class ReviewLogEntity(
    @PrimaryKey
    val id: String,                    // UUID
    val userId: String,
    val flashcardId: String,
    val quality: Int,                  // 0 (Again), 2 (Hard), 3 (Good), 5 (Easy)
    val responseTimeMs: Long? = null,
    val reviewedAt: Long = System.currentTimeMillis()
)
