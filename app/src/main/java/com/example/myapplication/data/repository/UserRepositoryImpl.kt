package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.UserDao
import com.example.myapplication.data.remote.JwtRoleParser
import com.example.myapplication.data.remote.TokenManager
import com.example.myapplication.data.remote.api.AuthApi
import com.example.myapplication.data.remote.dto.LoginRequest
import com.example.myapplication.data.remote.dto.RefreshRequest
import com.example.myapplication.data.remote.dto.RegisterRequest
import com.example.myapplication.domain.model.LoginResult
import com.example.myapplication.domain.model.User
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.data.remote.dto.AuthResponse
import com.example.myapplication.domain.model.toDomain
import com.example.myapplication.domain.repository.DeckRepository
import com.example.myapplication.domain.repository.ReviewLogRepository
import com.example.myapplication.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val tokenManager: TokenManager,
    private val deckRepository: DeckRepository,
    private val reviewLogRepository: ReviewLogRepository
) : UserRepository {

    override suspend fun login(email: String, password: String): LoginResult {
        val response = authApi.login(LoginRequest(email, password))
        val role = resolveRoleFromAuth(response)

        tokenManager.saveTokens(
            response.accessToken,
            response.refreshToken,
            response.userId,
            role.toApiString()
        )

        saveUserLocally(
            User(
                id = response.userId,
                email = email,
                displayName = email.substringBefore("@"),
                role = role
            )
        )

        if (role == UserRole.USER) {
            deckRepository.syncDecks(response.userId)
            // Sync review logs from server to restore stats
            reviewLogRepository.syncReviewLogs(response.userId)
        }

        return LoginResult(response.userId, role)
    }

    /** Prefer `role` inside JWT (always present with our API); fallback to JSON body. */
    private fun resolveRoleFromAuth(response: AuthResponse): UserRole {
        val fromJwt = JwtRoleParser.parseRole(response.accessToken)
        if (fromJwt != null) return UserRole.fromString(fromJwt)
        return UserRole.fromString(response.role)
    }

    override suspend fun register(email: String, password: String, displayName: String): String {
        val response = authApi.register(RegisterRequest(email, password, displayName))
        val role = resolveRoleFromAuth(response)

        tokenManager.saveTokens(
            response.accessToken,
            response.refreshToken,
            response.userId,
            role.toApiString()
        )

        saveUserLocally(
            User(
                id = response.userId,
                email = email,
                displayName = displayName,
                role = role
            )
        )

        deckRepository.syncDecks(response.userId)

        return response.userId
    }

    override suspend fun refreshToken() {
        val refreshToken = tokenManager.getRefreshToken()
            ?: throw IllegalStateException("No refresh token available")
        val response = authApi.refresh(RefreshRequest(refreshToken))
        val role = resolveRoleFromAuth(response)
        tokenManager.saveTokens(
            response.accessToken,
            response.refreshToken,
            response.userId,
            role.toApiString()
        )
    }

    override suspend fun logout() {
        tokenManager.clearTokens()
    }

    override suspend fun isLoggedIn(): Boolean =
        tokenManager.isLoggedIn()

    override suspend fun getCurrentUserId(): String? =
        tokenManager.getUserId()

    override suspend fun getCurrentUserRole(): UserRole {
        val token = tokenManager.getAccessToken() ?: return UserRole.USER
        JwtRoleParser.parseRole(token)?.let { return UserRole.fromString(it) }
        tokenManager.getRole()?.let { return UserRole.fromString(it) }
        val uid = tokenManager.getUserId() ?: return UserRole.USER
        return userDao.getUserById(uid)?.toDomain()?.role ?: UserRole.USER
    }

    override fun observeCurrentUser(): Flow<User?> {
        // In practice, we observe by stored userId
        // This is a simplified version
        return userDao.observeUserById("").map { it?.toDomain() }
    }

    override suspend fun getCurrentUser(): User? {
        val userId = tokenManager.getUserId() ?: return null
        return userDao.getUserById(userId)?.toDomain()
    }

    override suspend fun saveUserLocally(user: User) {
        val entity = com.example.myapplication.data.local.entity.UserEntity(
            id = user.id,
            email = user.email,
            passwordHash = "",  // Not stored locally — auth is JWT-based
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            role = user.role.toApiString(),
            subscriptionTier = user.subscriptionTier.name.lowercase(),
            aiUsageToday = user.aiUsageToday,
            isActive = user.isActive,
            createdAt = user.createdAt
        )
        // Use UPDATE if user exists to avoid CASCADE delete of all related data
        val existing = userDao.getUserById(user.id)
        if (existing != null) {
            userDao.updateUser(entity)
        } else {
            userDao.insertUser(entity)
        }
    }

    // ── Forgot Password ──

    override suspend fun forgotPassword(email: String): String {
        val response = authApi.forgotPassword(
            com.example.myapplication.data.remote.dto.ForgotPasswordRequest(email)
        )
        return response.message
    }

    override suspend fun resetPassword(email: String, token: String, newPassword: String): String {
        val response = authApi.resetPassword(
            com.example.myapplication.data.remote.dto.ResetPasswordRequest(email, token, newPassword)
        )
        return response.message
    }

    // ── Google Sign-In ──

    override suspend fun googleLogin(idToken: String): LoginResult {
        val response = authApi.googleLogin(
            com.example.myapplication.data.remote.dto.GoogleLoginRequest(idToken)
        )
        val role = resolveRoleFromAuth(response)

        tokenManager.saveTokens(
            response.accessToken,
            response.refreshToken,
            response.userId,
            role.toApiString()
        )

        saveUserLocally(
            User(
                id = response.userId,
                email = "google_user",  // Will be overwritten by sync
                displayName = "Google User",
                role = role
            )
        )

        if (role == UserRole.USER) {
            deckRepository.syncDecks(response.userId)
            reviewLogRepository.syncReviewLogs(response.userId)
        }

        return LoginResult(response.userId, role)
    }

    // ── Update Email ──────────────────────────────────────────

    override suspend fun updateEmail(newEmail: String): String {
        val response = authApi.updateEmail(com.example.myapplication.data.remote.dto.UpdateEmailRequest(newEmail))
        // Update local DB
        val userId = getCurrentUserId()
        if (userId != null) {
            val user = getCurrentUser()
            if (user != null) {
                saveUserLocally(user.copy(email = newEmail))
            }
        }
        return response.message
    }
}
