package com.example.data.repository

import com.example.core.security.ApprovalGuard
import com.example.core.security.GuardDecision
import com.example.core.security.SecretVault
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
import java.util.concurrent.TimeUnit

class BackendRepository(private val vault: SecretVault) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private fun buildService(baseUrl: String): BackendRunnerService {
        val cleanUrl = baseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }
        require(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) {
            "يجب أن يبدأ رابط خادم Backend بـ https:// أو http://"
        }
        return Retrofit.Builder()
            .baseUrl(cleanUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BackendRunnerService::class.java)
    }

    private fun authHeader(): String {
        val token = vault.getSecret(SecretVault.KEY_BACKEND_TOKEN)
        return if (token.isNotBlank()) "Bearer $token" else ""
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
