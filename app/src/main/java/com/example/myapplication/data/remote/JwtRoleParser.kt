package com.example.myapplication.data.remote

import android.util.Base64
import org.json.JSONObject

/**
 * Reads the `role` claim from a JWT payload (no signature verification).
 * Matches ASP.NET claim type "role".
 */
object JwtRoleParser {

    fun parseRole(accessToken: String): String? {
        val parts = accessToken.split('.')
        if (parts.size < 2) return null
        return try {
            var payload = parts[1]
            val pad = payload.length % 4
            if (pad != 0) payload += "=".repeat(4 - pad)
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
            val json = JSONObject(String(decoded, Charsets.UTF_8))
            when {
                json.has("role") && !json.isNull("role") -> json.getString("role")
                json.has("Role") && !json.isNull("Role") -> json.getString("Role")
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
