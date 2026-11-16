package com.example.data.repository

import com.example.BuildConfig
import com.example.core.security.SecretVault
import com.example.data.remote.AuthSessionService
import com.example.data.remote.GoogleSessionRequest
import com.example.data.remote.RefreshSessionRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        saveSession(response.accessToken, response.refreshToken, user.id, user.email, user.displayName)
        user.id
    }.rethrowCancellation()

    suspend fun restoreSession(expectedAccessToken: String? = null): Result<String> = runCatching {
        refreshMutex.withLock {
            val currentAccessToken = vault.getSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN)
            // Another request may already have rotated the refresh token while this request waited.
            if (expectedAccessToken != null && currentAccessToken != expectedAccessToken) {
                val currentUserId = vault.getSecret(SecretVault.KEY_AUTH_USER_ID)
                require(currentAccessToken.isNotBlank() && currentUserId.isNotBlank()) {
                    "انتهت جلسة AI I.Z. سجّل الدخول مجدداً."
                }
                return@withLock currentUserId
            }
            val refreshToken = vault.getSecret(SecretVault.KEY_AUTH_REFRESH_TOKEN)
            require(refreshToken.isNotBlank()) { "لا توجد جلسة محفوظة." }
            val response = service.refresh(RefreshSessionRequest(refreshToken))
            vault.putSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN, response.accessToken)
            vault.putSecret(SecretVault.KEY_AUTH_REFRESH_TOKEN, response.refreshToken)
            val profile = service.me("Bearer ${response.accessToken}").user
            vault.putSecret(SecretVault.KEY_AUTH_USER_ID, profile.id)
            vault.putSecret(SecretVault.KEY_AUTH_EMAIL, profile.email)
            vault.putSecret(SecretVault.KEY_AUTH_DISPLAY_NAME, profile.displayName)
            profile.id
        }
    }.rethrowCancellation()

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val accessToken = vault.getSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN)
        require(accessToken.isNotBlank()) { "انتهت جلسة AI I.Z. سجّل الدخول مجدداً." }
        service.deleteAccount("Bearer $accessToken", com.example.data.remote.DeleteAccountRequest())
        // Account deletion is permanent server-side; wipe all local vault material for this install.
        vault.clearAll()
        Unit
    }.rethrowCancellation()

    suspend fun signOut() {
        val refreshToken = vault.getSecret(SecretVault.KEY_AUTH_REFRESH_TOKEN)
        if (refreshToken.isNotBlank()) runCatching { service.logout(RefreshSessionRequest(refreshToken)) }
        vault.putSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN, "")
        vault.putSecret(SecretVault.KEY_AUTH_REFRESH_TOKEN, "")
        vault.putSecret(SecretVault.KEY_AUTH_USER_ID, "")
        vault.putSecret(SecretVault.KEY_AUTH_EMAIL, "")
        vault.putSecret(SecretVault.KEY_AUTH_DISPLAY_NAME, "")
    }

    private fun saveSession(accessToken: String, refreshToken: String, userId: String, email: String, displayName: String) {
        require(accessToken.isNotBlank() && refreshToken.isNotBlank() && userId.isNotBlank()) { "استجابة جلسة AI I.Z غير مكتملة." }
        vault.putSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN, accessToken)
        vault.putSecret(SecretVault.KEY_AUTH_REFRESH_TOKEN, refreshToken)
        vault.putSecret(SecretVault.KEY_AUTH_USER_ID, userId)
        vault.putSecret(SecretVault.KEY_AUTH_EMAIL, email)
        vault.putSecret(SecretVault.KEY_AUTH_DISPLAY_NAME, displayName)
    }

    companion object {
        // Shared across repository instances so parallel API 401 responses rotate once per process.
        private val refreshMutex = Mutex()
    }

    private fun <T> Result<T>.rethrowCancellation(): Result<T> {
        val error = exceptionOrNull()
        if (error is CancellationException) throw error
        return this
    }
}
