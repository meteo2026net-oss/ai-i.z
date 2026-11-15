package com.example.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UserDataService {
    @GET("api/data/{collection}")
    suspend fun listRecords(
        @Path("collection") collection: String,
        @Query("limit") limit: Int = 100,
        @Header("Authorization") authorization: String
    ): ResponseBody

    @GET("api/data/{collection}/{recordId}")
    suspend fun getRecord(
        @Path("collection") collection: String,
        @Path("recordId") recordId: String,
        @Header("Authorization") authorization: String
    ): ResponseBody

    @PUT("api/data/{collection}/{recordId}")
    suspend fun putRecord(
        @Path("collection") collection: String,
        @Path("recordId") recordId: String,
        @Body body: RequestBody,
        @Header("Authorization") authorization: String
    ): ResponseBody

    @DELETE("api/data/{collection}/{recordId}")
    suspend fun deleteRecord(
        @Path("collection") collection: String,
        @Path("recordId") recordId: String,
        @Header("Authorization") authorization: String
    )
}
