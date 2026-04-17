package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.SharedImageDto
import com.example.myapplication.data.remote.dto.ShareImageRequest
import com.example.myapplication.data.remote.dto.UploadImageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Community image library API — search/share images by keyword.
 */
interface SharedImageApi {
    @GET("api/shared-images")
    suspend fun search(@Query("keyword") keyword: String, @Query("limit") limit: Int = 10): List<SharedImageDto>

    @POST("api/shared-images")
    suspend fun share(@Body request: ShareImageRequest)

    @POST("api/shared-images/{id}/use")
    suspend fun incrementUsage(@Path("id") id: String)

    @Multipart
    @POST("api/shared-images/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("keyword") keyword: RequestBody
    ): UploadImageResponse
}
