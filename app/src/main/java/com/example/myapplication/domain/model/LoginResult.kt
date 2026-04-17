package com.example.myapplication.domain.model

/** Result of a successful login — use [role] for navigation (admin vs user). */
data class LoginResult(
    val userId: String,
    val role: UserRole
)
