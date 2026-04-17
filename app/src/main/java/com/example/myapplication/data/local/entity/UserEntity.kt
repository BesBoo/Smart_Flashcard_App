package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    val id: String,                    // UUID from server
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val avatarUrl: String? = null,

    /** "user" | "admin" — mirrors server Users.Role */
    val role: String = "user",

    // Subscription & AI usage
    val subscriptionTier: String = "Free",   // "Free" | "Premium"
    val aiUsageToday: Int = 0,
    val aiUsageResetDate: Long = System.currentTimeMillis(),

    // Status & timestamps
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
