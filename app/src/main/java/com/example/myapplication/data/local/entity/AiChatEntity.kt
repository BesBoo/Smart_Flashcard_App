package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_chat_history",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "sessionId", "createdAt"])
    ]
)
data class AiChatEntity(
    @PrimaryKey
    val id: String,                    // UUID
    val userId: String,
    val sessionId: String,             // Group messages into conversations
    val role: String,                  // "user" | "assistant"
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
