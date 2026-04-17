package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.SyncPullResponse
import com.example.myapplication.data.remote.dto.SyncPushRequest
import com.example.myapplication.data.remote.dto.SyncPushResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SyncApi {

    @POST("api/sync/push")
    suspend fun push(@Body request: SyncPushRequest): SyncPushResponse

    @GET("api/sync/pull")
    suspend fun pull(@Query("since") since: String): SyncPullResponse
}
