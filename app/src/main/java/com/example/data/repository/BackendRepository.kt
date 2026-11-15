package com.example.data.repository

import com.example.BuildConfig
import com.example.core.security.ApprovalGuard
import com.example.core.security.GuardDecision
import com.example.core.security.SecretVault
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.data.remote.BackendRunnerService
import com.example.domain.model.BackendHealthResponse
import com.example.domain.model.BackendTestRunRequest
import com.example.domain.model.BackendTestRunResponse
import com.example.domain.model.DbSchemaResponse
import com.example.domain.model.DbSqlRequest
import com.example.domain.model.DbSqlResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.URI
import java.util.concurrent.TimeUnit

internal object BackendUrlPolicy {
    private val localDevelopmentHosts = setOf("localhost", "127.0.0.1", "10.0.2.2")

    fun normalizeAndValidate(baseUrl: String, allowLocalHttp: Boolean): String {
        val cleanUrl = baseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }
        val uri = runCatching { URI(cleanUrl) }.getOrNull()
            ?: throw IllegalArgumentException("رابط Backend غير صالح.")
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        require(host != null && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
            "يجب إدخال رابط Backend صالح دون بيانات اعتماد أو معاملات URL."
        }
        val isHttps = scheme == "https"
        val isLocalDebugHttp = allowLocalHttp && scheme == "http" && host in localDevelopmentHosts
        require(isHttps || isLocalDebugHttp) {
            "يجب استخدام HTTPS لخادم Backend. يُسمح بـ HTTP فقط لعناوين loopback أثناء التطوير."
        }
        return cleanUrl
    }
}

class BackendRepository(private val vault: SecretVault) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private fun buildService(baseUrl: String): BackendRunnerService {
        val cleanUrl = BackendUrlPolicy.normalizeAndValidate(baseUrl, BuildConfig.DEBUG)
        return Retrofit.Builder()
            .baseUrl(cleanUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BackendRunnerService::class.java)
    }

    /**
     * Send a short-lived Firebase ID token. The backend verifies its signature,
     * audience, expiry, and revocation state; a static app-wide bearer secret is
     * never embedded in or sent by the Android client.
     */
    private suspend fun authHeader(): String = suspendCancellableCoroutine { continuation ->
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            continuation.resumeWithException(
                SecurityException("يجب تسجيل الدخول إلى Firebase قبل الاتصال بخادم Backend.")
            )
            return@suspendCancellableCoroutine
        }

        user.getIdToken(false).addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                val token = task.result?.token
                if (token.isNullOrBlank()) {
                    continuation.resumeWithException(
                        SecurityException("تعذر الحصول على رمز جلسة Firebase صالح.")
                    )
                } else {
                    continuation.resume("Bearer $token")
                }
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("تعذر التحقق من جلسة Firebase.")
                )
            }
        }
    }

    suspend fun checkHealth(
        baseUrl: String,
        extraProtectedTargets: List<String> = emptyList()
    ): Result<BackendHealthResponse> = runCatching {
        val res = buildService(baseUrl).getHealth(authHeader())
        if (ApprovalGuard.isLegacyDatabaseTarget(res.activeDatabaseName, extraProtectedTargets)) {
            throw SecurityException("تم حظر الاتصال: الخادم يشير إلى قاعدة بيانات محمية ومعزولة.")
        }
        res
    }.rethrowCancellation()

    suspend fun runTests(
        baseUrl: String,
        repo: String,
        branch: String,
        target: String
    ): Result<BackendTestRunResponse> = runCatching {
        buildService(baseUrl).runTests(
            authHeader(),
            BackendTestRunRequest(repo = repo, branch = branch, target = target)
        )
    }.rethrowCancellation()

    suspend fun fetchDbSchema(
        baseUrl: String,
        extraProtectedTargets: List<String> = emptyList()
    ): Result<DbSchemaResponse> = runCatching {
        val schema = buildService(baseUrl).getDbSchema(authHeader())
        if (ApprovalGuard.isLegacyDatabaseTarget(schema.databaseName, extraProtectedTargets)) {
            throw SecurityException("محظور أمنياً: قاعدة البيانات المحمية خارج نطاق التطبيق تماماً.")
        }
        schema
    }.rethrowCancellation()

    suspend fun executeReadOnlySql(
        baseUrl: String,
        targetDatabase: String,
        sql: String,
        extraProtectedTargets: List<String> = emptyList()
    ): Result<DbSqlResponse> = runCatching {
        when (val decision = ApprovalGuard.validateSqlExecution(
            sql = sql,
            targetDatabase = targetDatabase,
            hasExplicitTier3Approval = false,
            extraProtectedTargets = extraProtectedTargets
        )) {
            is GuardDecision.Blocked -> throw SecurityException(decision.reason)
            is GuardDecision.RequiresApproval -> throw SecurityException(decision.reason)
            GuardDecision.AllowedAuto -> Unit
        }
        buildService(baseUrl).runReadOnlySql(
            authHeader(),
            DbSqlRequest(targetDatabase = targetDatabase, sql = sql)
        )
    }.rethrowCancellation()

    suspend fun executeApprovedSql(
        baseUrl: String,
        targetDatabase: String,
        sql: String,
        approvalId: String,
        extraProtectedTargets: List<String> = emptyList()
    ): Result<DbSqlResponse> = runCatching {
        if (approvalId.isBlank()) {
            throw SecurityException("يتطلب تنفيذ SQL على القاعدة المستضافة معرف موافقة صريحة من المستوى الثالث.")
        }
        when (val decision = ApprovalGuard.validateSqlExecution(
            sql = sql,
            targetDatabase = targetDatabase,
            hasExplicitTier3Approval = true,
            extraProtectedTargets = extraProtectedTargets
        )) {
            is GuardDecision.Blocked -> throw SecurityException(decision.reason)
            is GuardDecision.RequiresApproval -> throw SecurityException(decision.reason)
            GuardDecision.AllowedAuto -> Unit
        }
        buildService(baseUrl).executeApprovedSql(
            authHeader(),
            DbSqlRequest(targetDatabase = targetDatabase, sql = sql, approvalId = approvalId)
        )
    }.rethrowCancellation()

    private fun <T> Result<T>.rethrowCancellation(): Result<T> {
        val ex = exceptionOrNull()
        if (ex is CancellationException) throw ex
        return this
    }
}
