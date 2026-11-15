package com.example.data.remote

import com.example.domain.model.BackendHealthResponse
import com.example.domain.model.BackendTestRunRequest
import com.example.domain.model.BackendTestRunResponse
import com.example.domain.model.DbSchemaResponse
import com.example.domain.model.DbSqlRequest
import com.example.domain.model.DbSqlResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface BackendRunnerService {
    @GET("api/health")
    suspend fun getHealth(
        @Header("Authorization") authHeader: String
    ): BackendHealthResponse

    @POST("api/tests/run")
    suspend fun runTests(
        @Header("Authorization") authHeader: String,
        @Body request: BackendTestRunRequest
    ): BackendTestRunResponse

    @GET("api/db/schema")
    suspend fun getDbSchema(
        @Header("Authorization") authHeader: String
    ): DbSchemaResponse

    @POST("api/db/query-readonly")
    suspend fun runReadOnlySql(
        @Header("Authorization") authHeader: String,
        @Body request: DbSqlRequest
    ): DbSqlResponse

    @POST("api/db/execute-approved")
    suspend fun executeApprovedSql(
        @Header("Authorization") authHeader: String,
        @Body request: DbSqlRequest
    ): DbSqlResponse
}
