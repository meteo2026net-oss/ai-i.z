package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.core.security.SecretVault
import com.example.data.remote.UserDataService
import com.example.domain.model.AgentTask
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.ChatMessage
import com.example.domain.model.OperationLog
import com.example.domain.model.RunnerApprovalRecord
import com.example.domain.model.RunnerApprovalOperation
import com.example.domain.model.UserSettings
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Authenticated PostgreSQL-backed data repository for AI I.Z.
 * All persistence is performed by the AI I.Z API; ownership is derived from
 * the authenticated server session and never trusted from a client userId.
 */
class UserDataRepository(context: Context) {
    private val vault = SecretVault(context.applicationContext)
    private val moshi = Moshi.Builder()
        .add(Date::class.java, object : JsonAdapter<Date>() {
            override fun fromJson(reader: com.squareup.moshi.JsonReader): Date? {
                if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) return reader.nextNull()
                val raw = reader.nextString()
                val formats = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                    "yyyy-MM-dd'T'HH:mm:ssX"
                )
                for (format in formats) {
                    val parsed = runCatching { SimpleDateFormat(format, Locale.US).parse(raw) }.getOrNull()
                    if (parsed != null) return parsed
                }
                return raw.toLongOrNull()?.let(::Date)
                    ?: throw IllegalArgumentException("Invalid server timestamp.")
            }
            override fun toJson(writer: com.squareup.moshi.JsonWriter, value: Date?) {
                if (value == null) writer.nullValue()
                else writer.value(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(value))
            }
        })
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val service: UserDataService by lazy {
        val baseUrl = BuildConfig.AI_IZ_API_BASE_URL.trim()
        require(baseUrl.startsWith("https://") || (BuildConfig.DEBUG && baseUrl.startsWith("http://10.0.2.2"))) {
            "خدمة بيانات AI I.Z غير مهيأة. يلزم ضبط عنوان HTTPS للخادم."
        }
        Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UserDataService::class.java)
    }

    private suspend fun <T> authorized(block: suspend (String) -> T): T {
        var token = vault.getSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN)
        require(token.isNotBlank()) { "انتهت جلسة AI I.Z. سجّل الدخول مجدداً." }
        return try {
            block("Bearer $token")
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
            AuthSessionRepository(vault).restoreSession(expectedAccessToken = token).getOrThrow()
            token = vault.getSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN)
            require(token.isNotBlank()) { "تعذر تجديد جلسة AI I.Z." }
            block("Bearer $token")
        }
    }

    private suspend fun <T : Any> fetchList(collection: String, limit: Int, adapter: JsonAdapter<T>): List<T> =
        authorized { bearer ->
            val root = moshi.adapter(Map::class.java).fromJson(service.listRecords(collection, limit, bearer).string())
            val records = root?.get("records") as? List<*> ?: emptyList<Any>()
            records.mapNotNull { adapter.fromJsonValue(it) }
        }

    private suspend fun <T : Any> fetchOne(collection: String, id: String, adapter: JsonAdapter<T>): T? =
        authorized { bearer ->
            val root = moshi.adapter(Map::class.java).fromJson(service.getRecord(collection, id, bearer).string())
            adapter.fromJsonValue(root?.get("record"))
        }

    private suspend fun <T : Any> save(collection: String, id: String, value: T, adapter: JsonAdapter<T>) {
        val body = adapter.toJson(value).toRequestBody("application/json; charset=utf-8".toMediaType())
        authorized { bearer -> service.putRecord(collection, id, body, bearer) }
    }

    private fun <T> observe(pollMillis: Long = 15_000, loader: suspend () -> T): Flow<T> = flow {
        while (currentCoroutineContext().isActive) {
            emit(loader())
            delay(pollMillis)
        }
    }

    private val settingsAdapter by lazy { moshi.adapter(UserSettings::class.java) }
    private val taskAdapter by lazy { moshi.adapter(AgentTask::class.java) }
    private val approvalAdapter by lazy { moshi.adapter(ApprovalRequest::class.java) }
    private val chatAdapter by lazy { moshi.adapter(ChatMessage::class.java) }
    private val logAdapter by lazy { moshi.adapter(OperationLog::class.java) }
    private val studioProjectAdapter by lazy {
        moshi.adapter(com.example.domain.model.StudioProjectSpec::class.java)
    }

    /** Durable, account-scoped Studio project storage on the AI I.Z API. */
    suspend fun saveStudioProject(project: com.example.domain.model.StudioProjectSpec): Result<Unit> = result {
        require(project.id.matches(Regex("^[A-Za-z0-9_.:-]{1,128}$"))) { "Invalid Studio project ID." }
        require(project.files.size in 1..24) { "Studio project file count is outside the supported limit." }
        val totalBytes = project.files.sumOf { it.content.toByteArray(Charsets.UTF_8).size }
        require(totalBytes <= 220_000) { "Studio project exceeds the cloud storage limit." }
        save("studio_projects", project.id, project, studioProjectAdapter)
    }

    suspend fun listStudioProjects(): Result<List<com.example.domain.model.StudioProjectSpec>> = result {
        fetchList("studio_projects", 50, studioProjectAdapter)
    }

    suspend fun getStudioProject(projectId: String): Result<com.example.domain.model.StudioProjectSpec?> = result {
        require(projectId.matches(Regex("^[A-Za-z0-9_.:-]{1,128}$"))) { "Invalid Studio project ID." }
        try { fetchOne("studio_projects", projectId, studioProjectAdapter) }
        catch (error: HttpException) { if (error.code() == 404) null else throw error }
    }

    fun observeSettings(userId: String): Flow<UserSettings> = observe {
        fetchOne("settings", "profile", settingsAdapter) ?: UserSettings(id = "profile", userId = userId)
    }

    suspend fun saveSettings(settings: UserSettings): Result<Unit> = result {
        val normalized = settings.copy(
            id = "profile",
            activeRepo = settings.activeRepo.take(128),
            activeBranch = settings.activeBranch.take(128),
            aiProvider = if (settings.aiProvider == "openai") "openai" else "gemini",
            aiModel = settings.aiModel.ifBlank { "gemini-3.8-flash" }.take(64),
            backendUrl = settings.backendUrl.take(300)
        )
        save("settings", "profile", normalized, settingsAdapter)
    }

    fun observeTasks(userId: String): Flow<List<AgentTask>> = observe {
        fetchList("tasks", 200, taskAdapter)
    }

    suspend fun createTask(task: AgentTask, explicitId: String? = null): Result<String> = result {
        val id = explicitId ?: "task_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        save("tasks", id, normalizeTask(task.copy(id = id)), taskAdapter)
        id
    }

    suspend fun updateTask(task: AgentTask): Result<Unit> = result {
        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        save("tasks", task.id, normalizeTask(task), taskAdapter)
    }

    suspend fun getTaskById(taskId: String, userId: String? = null): Result<AgentTask?> = result {
        try { fetchOne("tasks", taskId, taskAdapter) }
        catch (error: HttpException) { if (error.code() == 404) null else throw error }
    }

    private fun RunnerApprovalRecord.toApprovalRequest(userId: String): ApprovalRequest {
        val operation = operation ?: emptyMap()
        val tierThree = actionType in setOf("merge_pr", "db_sql", "db_migration", "db_rls_grants", "prod_deploy")
        return ApprovalRequest(
            id = id,
            userId = userId,
            taskId = operation["taskId"]?.toString().orEmpty(),
            tier = if (tierThree) 3L else 2L,
            actionType = actionType,
            title = (operation["title"] as? String)?.take(200) ?: preview.take(200),
            description = preview.take(2000),
            payloadPreview = (operation["sql"] as? String)
                ?: (operation["body"] as? String)
                ?: preview,
            targetResource = targetResource,
            status = status,
            decisionNote = decisionNote,
            operationPayload = operation,
            operationHash = operationHash,
            serverOwned = true
        )
    }

    fun observeApprovals(userId: String): Flow<List<ApprovalRequest>> = observe {
        listRunnerApprovals(100).getOrThrow().map { it.toApprovalRequest(userId) }
    }

    suspend fun createApproval(approval: ApprovalRequest): Result<String> = result {
        require(approval.operationPayload.isNotEmpty()) {
            "Approval requires a complete immutable operation payload; legacy approvals are disabled."
        }
        val normalizedOperation = approval.operationPayload.toMutableMap().apply {
            put("taskId", approval.taskId)
            put("title", approval.title.take(200))
        }
        val created = createRunnerApproval(
            RunnerApprovalOperation(
                actionType = approval.actionType,
                targetResource = approval.targetResource.take(300),
                operation = normalizedOperation,
                preview = (approval.payloadPreview.ifBlank { approval.description.ifBlank { approval.title } }).take(4000)
            )
        ).getOrThrow()
        created.id
    }

    suspend fun decideApproval(
        approvalId: String,
        newStatus: String,
        decisionNote: String,
        userId: String? = null
    ): Result<Unit> = result {
        require(newStatus in setOf("approved", "rejected")) {
            "Only explicit approval or rejection is accepted here. Execution must be performed by the isolated Runner."
        }
        decideRunnerApproval(approvalId, newStatus, decisionNote).getOrThrow()
    }

    /**
     * Server-owned approval API. These methods are intentionally separate from
     * legacy user_records approvals until callers supply a complete immutable
     * operation payload. Never execute from locally reconstructed state.
     */
    suspend fun createRunnerApproval(request: com.example.domain.model.RunnerApprovalOperation): Result<com.example.domain.model.RunnerApprovalRecord> = result {
        require(request.actionType.isNotBlank() && request.targetResource.isNotBlank())
        require(request.operation.isNotEmpty() && request.preview.isNotBlank())
        val body = moshi.adapter(com.example.domain.model.RunnerApprovalOperation::class.java)
            .toJson(request).toRequestBody("application/json; charset=utf-8".toMediaType())
        authorized { bearer ->
            val root = moshi.adapter(com.example.domain.model.RunnerApprovalEnvelope::class.java)
                .fromJson(service.createRunnerApproval(body, bearer).string())
                ?: error("Empty Runner approval response.")
            root.approval
        }
    }

    suspend fun listRunnerApprovals(limit: Int = 50): Result<List<com.example.domain.model.RunnerApprovalRecord>> = result {
        authorized { bearer ->
            val root = moshi.adapter(com.example.domain.model.RunnerApprovalListEnvelope::class.java)
                .fromJson(service.listRunnerApprovals(limit.coerceIn(1, 100), bearer).string())
                ?: error("Empty Runner approval list.")
            root.approvals
        }
    }

    suspend fun decideRunnerApproval(
        approvalId: String,
        decision: String,
        note: String = ""
    ): Result<com.example.domain.model.RunnerApprovalRecord> = result {
        require(decision in setOf("approved", "rejected"))
        val request = com.example.domain.model.RunnerApprovalDecision(decision, note.take(500))
        val body = moshi.adapter(com.example.domain.model.RunnerApprovalDecision::class.java)
            .toJson(request).toRequestBody("application/json; charset=utf-8".toMediaType())
        authorized { bearer ->
            val root = moshi.adapter(com.example.domain.model.RunnerApprovalEnvelope::class.java)
                .fromJson(service.decideRunnerApproval(approvalId, body, bearer).string())
                ?: error("Empty Runner approval decision response.")
            root.approval
        }
    }

    suspend fun consumeRunnerApproval(
        approvalId: String,
        actionType: String,
        targetResource: String,
        operationHash: String
    ): Result<com.example.domain.model.RunnerApprovalRecord> = result {
        require(operationHash.matches(Regex("^[0-9a-f]{64}$")))
        val request = com.example.domain.model.RunnerApprovalConsumeRequest(actionType, targetResource, operationHash)
        val body = moshi.adapter(com.example.domain.model.RunnerApprovalConsumeRequest::class.java)
            .toJson(request).toRequestBody("application/json; charset=utf-8".toMediaType())
        authorized { bearer ->
            val root = moshi.adapter(com.example.domain.model.RunnerApprovalConsumeEnvelope::class.java)
                .fromJson(service.consumeRunnerApproval(approvalId, body, bearer).string())
                ?: error("Empty Runner approval consumption response.")
            root.approval
        }
    }

    suspend fun reportRunnerApproval(
        approvalId: String,
        outcome: String,
        note: String = ""
    ): Result<com.example.domain.model.RunnerApprovalReportEnvelope> = result {
        require(outcome in setOf("success", "failed"))
        val request = com.example.domain.model.RunnerApprovalReport(outcome, note.take(500))
        val body = moshi.adapter(com.example.domain.model.RunnerApprovalReport::class.java)
            .toJson(request).toRequestBody("application/json; charset=utf-8".toMediaType())
        authorized { bearer ->
            moshi.adapter(com.example.domain.model.RunnerApprovalReportEnvelope::class.java)
                .fromJson(service.reportRunnerApproval(approvalId, body, bearer).string())
                ?: error("Empty Runner execution report response.")
        }
    }

    fun observeChatMessages(userId: String): Flow<List<ChatMessage>> = observe {
        fetchList("chat_messages", 200, chatAdapter).sortedBy { it.createdAt?.time ?: 0L }
    }

    suspend fun addChatMessage(message: ChatMessage): Result<String> = result {
        val id = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val normalized = message.copy(
            id = id,
            role = if (message.role in listOf("user", "assistant", "system")) message.role else "user",
            content = message.content.ifBlank { "..." }.take(12000),
            repoContext = message.repoContext.take(128),
            linkedTaskId = message.linkedTaskId.take(128)
        )
        save("chat_messages", id, normalized, chatAdapter)
        id
    }

    fun observeLogs(userId: String): Flow<List<OperationLog>> = observe {
        fetchList("operation_logs", 150, logAdapter)
    }

    suspend fun addLog(log: OperationLog): Result<String> = result {
        val id = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val normalized = log.copy(
            id = id,
            category = if (log.category in setOf("agent", "github", "security", "ai", "backend", "test")) log.category else "agent",
            tier = log.tier.coerceIn(1L, 3L),
            action = log.action.ifBlank { "عملية" }.take(200),
            details = log.details.take(3000),
            outcome = if (log.outcome in setOf("success", "failed", "error", "blocked", "pending")) log.outcome else "failed",
            repo = log.repo.take(128)
        )
        save("operation_logs", id, normalized, logAdapter)
        id
    }

    private fun normalizeTask(task: AgentTask): AgentTask = task.copy(
        repo = task.repo.take(128),
        branch = task.branch.take(128),
        title = task.title.ifBlank { "مهمة AI I.Z" }.take(200),
        prompt = task.prompt.ifBlank { "فحص المشروع" }.take(2000),
        lastCompletedStep = task.lastCompletedStep.take(300),
        lastResultSummary = task.lastResultSummary.take(2000),
        lastError = task.lastError.take(1000),
        modifiedFiles = task.modifiedFiles.take(30).map { it.take(500) },
        nextSteps = task.nextSteps.take(30).map { it.take(500) }
    )

    private suspend fun <T> result(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
