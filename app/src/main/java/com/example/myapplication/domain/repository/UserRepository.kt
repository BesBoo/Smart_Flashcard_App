package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.LoginResult
import com.example.myapplication.domain.model.User
import com.example.myapplication.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun login(email: String, password: String): LoginResult
    suspend fun register(email: String, password: String, displayName: String): String  // returns userId
    suspend fun refreshToken()
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    suspend fun getCurrentUserId(): String?

    /** Role from session (DataStore); defaults to [UserRole.USER] if missing. */
    suspend fun getCurrentUserRole(): UserRole
    fun observeCurrentUser(): Flow<User?>
    suspend fun getCurrentUser(): User?
    suspend fun saveUserLocally(user: User)

    // ── Forgot Password ──
    suspend fun forgotPassword(email: String): String  // returns message
    suspend fun resetPassword(email: String, token: String, newPassword: String): String

    // ── Google Sign-In ──
    suspend fun googleLogin(idToken: String): LoginResult
}
