package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_metadata",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "isSynced"])
    ]
)
data class SyncMetadataEntity(
    @PrimaryKey
    val id: String,                    // UUID
    val userId: String,
    val entityType: String,            // "deck" | "flashcard" | "review_log" | "ai_chat"
    val entityId: String,
    val action: String,                // "CREATE" | "UPDATE" | "DELETE"
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
