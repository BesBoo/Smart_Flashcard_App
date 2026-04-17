package com.example.myapplication.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that injects the JWT Bearer token into every request.
 * Skips auth endpoints (login, register, refresh) which don't require a token.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    private val noAuthPaths = listOf(
        "api/auth/login",
        "api/auth/register",
        "api/auth/refresh"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip token injection for auth endpoints
        val path = originalRequest.url.encodedPath
        if (noAuthPaths.any { path.contains(it) }) {
            return chain.proceed(originalRequest)
        }

        // Get token (blocking is acceptable for an interceptor)
        val token = runBlocking { tokenManager.getAccessToken() }

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
