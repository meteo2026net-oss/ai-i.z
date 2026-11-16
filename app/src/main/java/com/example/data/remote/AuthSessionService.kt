package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class GoogleSessionRequest(val idToken: String)
data class RefreshSessionRequest(val refreshToken: String)
data class AuthSessionResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long, val user: AuthUserDto? = null)
data class AuthUserDto(val id: String, val email: String, val displayName: String = "", val avatarUrl: String? = null)
data class AuthMeResponse(val user: AuthUserDto)

data class DeleteAccountRequest(val confirm: String = "DELETE_MY_ACCOUNT")

interface AuthSessionService {
    @POST("api/auth/session") suspend fun createSession(@Body request: GoogleSessionRequest): AuthSessionResponse
    @POST("api/auth/refresh") suspend fun refresh(@Body request: RefreshSessionRequest): AuthSessionResponse
    @POST("api/auth/logout") suspend fun logout(@Body request: RefreshSessionRequest)
    @GET("api/me") suspend fun me(@Header("Authorization") authorization: String): AuthMeResponse
    @POST("api/account/delete") suspend fun deleteAccount(
        @Header("Authorization") authorization: String,
        @Body request: DeleteAccountRequest
    )
}
