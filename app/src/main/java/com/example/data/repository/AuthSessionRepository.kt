package com.example.data.repository

import com.example.BuildConfig
import com.example.core.security.SecretVault
import com.example.data.remote.AuthSessionService
import com.example.data.remote.GoogleSessionRequest
import com.example.data.remote.RefreshSessionRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AuthSessionRepository(private val vault: SecretVault) {
    private val service: AuthSessionService by lazy {
        val baseUrl = BuildConfig.AI_IZ_API_BASE_URL.trim()
        require(baseUrl.startsWith("https://") || (BuildConfig.DEBUG && baseUrl.startsWith("http://10.0.2.2"))) {
            "خدمة حساب AI I.Z غير مهيأة. يلزم ضبط عنوان HTTPS للخادم قبل تسجيل الدخول."
        }
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
        Retrofit.Builder().baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/").client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(AuthSessionService::class.java)
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<String> = runCatching {
        require(idToken.isNotBlank()) { "لم يتم استلام رمز هوية Google." }
        val response = service.createSession(GoogleSessionRequest(idToken))
        val user = requireNotNull(response.user) { "استجابة الخادم لا تحتوي على ملف الحساب." }
        saveSession(response.accessToken, response.refreshToken, user.id)
        user.id
    }.rethrowCancellation()

    suspend fun restoreSession(): Result<String> = runCatching {
        val refreshToken = vault.getSecret(SecretVault.KEY_AUTH_REFRESH_TOKEN)
        require(refreshToken.isNotBlank()) { "لا توجد جلسة محفوظة." }
        val response = service.refresh(RefreshSessionRequest(refreshToken))
        vault.putSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN, response.accessToken)
        vault.putSecret(SecretVault.KEY_AUTH_REFRESH_TOKEN, response.refreshToken)
        val profile = service.me("Bearer ${response.accessToken}").user
        vault.putSecret(SecretVault.KEY_AUTH_USER_ID, profile.id)
        profile.id
    }.rethrowCancellation()

    suspend fun signOut() {
        val accessToken = vault.getSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN)
        if (accessToken.isNotBlank()) runCatching { service.logout("Bearer $accessToken") }
        vault.putSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN, "")
        vault.putSecret(SecretVault.KEY_AUTH_REFRESH_TOKEN, "")
        vault.putSecret(SecretVault.KEY_AUTH_USER_ID, "")
    }

    private fun saveSession(accessToken: String, refreshToken: String, userId: String) {
        require(accessToken.isNotBlank() && refreshToken.isNotBlank() && userId.isNotBlank()) { "استجابة جلسة AI I.Z غير مكتملة." }
        vault.putSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN, accessToken)
        vault.putSecret(SecretVault.KEY_AUTH_REFRESH_TOKEN, refreshToken)
        vault.putSecret(SecretVault.KEY_AUTH_USER_ID, userId)
    }

    private fun <T> Result<T>.rethrowCancellation(): Result<T> {
        val error = exceptionOrNull()
        if (error is CancellationException) throw error
        return this
    }
}
