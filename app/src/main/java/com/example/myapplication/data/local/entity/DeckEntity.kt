package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "decks",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class DeckEntity(
    @PrimaryKey
    val id: String,                    // UUID from server
    val userId: String,
    val name: String,
    val description: String? = null,
    val coverImageUrl: String? = null,

    // Sharing
    val isOwner: Boolean = true,
    val permission: String? = null,       // null / "read" / "edit"
    val ownerName: String? = null,
    val shareCode: String? = null,
    val isShared: Boolean = false,
    val googleSheetUrl: String? = null,

    // Soft delete & timestamps
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
