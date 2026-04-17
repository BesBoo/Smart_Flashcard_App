package com.example.myapplication.data.remote

import com.example.myapplication.data.remote.dto.AuthResponse
import com.example.myapplication.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * On HTTP 401, refreshes the access token via plain HTTP (avoids Retrofit/OkHttp cycles)
 * and retries the request so [AuthInterceptor] can attach the new Bearer token.
 */
@Singleton
class TokenRefreshInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val json: Json,
    @Named("plain") private val plainClient: OkHttpClient
) : Interceptor {

    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)

        if (response.code != 401) return response
        if (request.header(HEADER_AUTH_RETRY) == "1") return response
        if (request.url.encodedPath.contains("api/auth/")) return response

        response.close()

        synchronized(refreshLock) {
            if (!refreshTokens()) {
                return chain.proceed(
                    request.newBuilder().header(HEADER_AUTH_RETRY, "1").build()
                )
            }
        }

        // AuthInterceptor runs *outside* this interceptor, so a plain retry would still send
        // the old Bearer token. Attach the new access token explicitly.
        val newAccess = runBlocking { tokenManager.getAccessToken() }
            ?: return chain.proceed(request.newBuilder().header(HEADER_AUTH_RETRY, "1").build())

        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .build()
        )
    }

    private fun refreshTokens(): Boolean {
        val refreshToken = runBlocking { tokenManager.getRefreshToken() } ?: return false
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
        val httpRequest = Request.Builder()
            .url("${NetworkConfig.BASE_URL}api/auth/refresh")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            plainClient.newCall(httpRequest).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) return false
                val bodyString = refreshResponse.body?.string() ?: return false
                val auth = json.decodeFromString<AuthResponse>(bodyString)
                runBlocking {
                    tokenManager.saveTokens(
                        auth.accessToken,
                        auth.refreshToken,
                        auth.userId,
                        auth.role
                    )
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val HEADER_AUTH_RETRY = "X-Auth-Retry"
    }
}
