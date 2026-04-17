package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.AuthResponse
import com.example.myapplication.data.remote.dto.ForgotPasswordRequest
import com.example.myapplication.data.remote.dto.GoogleLoginRequest
import com.example.myapplication.data.remote.dto.LoginRequest
import com.example.myapplication.data.remote.dto.MessageResponse
import com.example.myapplication.data.remote.dto.RefreshRequest
import com.example.myapplication.data.remote.dto.RegisterRequest
import com.example.myapplication.data.remote.dto.ResetPasswordRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): MessageResponse

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): MessageResponse

    @POST("api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): AuthResponse
}
