package com.example.myapplication.data.remote

/**
 * Base URL for Retrofit and one-off OkHttp calls (e.g. token refresh without Retrofit).
 * Must end with `/`.
 */
object NetworkConfig {
    const val BASE_URL = "http://10.0.2.2:5131/"
}
