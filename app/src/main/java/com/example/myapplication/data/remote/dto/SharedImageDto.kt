package com.example.myapplication.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SharedImageDto(
    val id: String,
    val imageUrl: String,
    val usageCount: Int
)

@Serializable
data class ShareImageRequest(
    val keyword: String,
    val imageUrl: String
)

@Serializable
data class UploadImageResponse(
    val imageUrl: String
)
