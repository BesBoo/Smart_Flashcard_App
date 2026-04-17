package com.example.myapplication.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token storage using EncryptedSharedPreferences.
 *
 * All sensitive data (JWT tokens, user ID, role) is encrypted at rest
 * using AES-256 GCM (keys) + AES-256 SIV (values), backed by
 * Android Keystore's master key.
 *
 * NFR-S04: Token lưu trữ trong EncryptedSharedPreferences.
 */
@Singleton
class TokenManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "secure_auth_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "role"
    }

    // ── Encrypted SharedPreferences instance ──
    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── StateFlows for reactive observation ──
    private val _accessTokenFlow = MutableStateFlow<String?>(null)
    val accessToken: Flow<String?> = _accessTokenFlow.asStateFlow()

    private val _refreshTokenFlow = MutableStateFlow<String?>(null)
    val refreshToken: Flow<String?> = _refreshTokenFlow.asStateFlow()

    private val _userIdFlow = MutableStateFlow<String?>(null)
    val userId: Flow<String?> = _userIdFlow.asStateFlow()

    init {
        // Load initial values from encrypted storage
        _accessTokenFlow.value = prefs.getString(KEY_ACCESS_TOKEN, null)
        _refreshTokenFlow.value = prefs.getString(KEY_REFRESH_TOKEN, null)
        _userIdFlow.value = prefs.getString(KEY_USER_ID, null)
    }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: String,
        role: String = "user"
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_ROLE, role)
            .apply()

        // Update flows
        _accessTokenFlow.value = accessToken
        _refreshTokenFlow.value = refreshToken
        _userIdFlow.value = userId
    }

    suspend fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    suspend fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    suspend fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    suspend fun getRole(): String? {
        return prefs.getString(KEY_ROLE, null)
    }

    suspend fun clearTokens() {
        prefs.edit().clear().apply()
        _accessTokenFlow.value = null
        _refreshTokenFlow.value = null
        _userIdFlow.value = null
    }

    suspend fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }
}
