package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.core.security.SecretVault
import com.example.data.remote.UserDataService
import com.example.domain.model.AgentTask
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.ChatMessage
import com.example.domain.model.OperationLog
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
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Compatibility name retained while the app migrates away from Firestore.
 * All persistence is performed by the AI I.Z API; ownership is derived from
 * the authenticated server session and never trusted from a client userId.
 */
class FirestoreAgentRepository(context: Context) {
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
                else writer.value(value.toInstant().toString())
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
            AuthSessionRepository(vault).restoreSession().getOrThrow()
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

    fun observeSettings(userId: String): Flow<UserSettings> = observe {
        fetchOne("settings", "profile", settingsAdapter) ?: UserSettings(id = "profile", userId = userId)
    }

    suspend fun saveSettings(settings: UserSettings): Result<Unit> = result {
        save("settings", "profile", settings.copy(id = "profile"), settingsAdapter)
    }

    fun observeTasks(userId: String): Flow<List<AgentTask>> = observe {
        fetchList("tasks", 200, taskAdapter)
    }

    suspend fun createTask(task: AgentTask, explicitId: String? = null): Result<String> = result {
        val id = explicitId ?: "task_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        save("tasks", id, task.copy(id = id), taskAdapter)
        id
    }

    suspend fun updateTask(task: AgentTask): Result<Unit> = result {
        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        save("tasks", task.id, task, taskAdapter)
    }

    suspend fun getTaskById(taskId: String, userId: String? = null): Result<AgentTask?> = result {
        try { fetchOne("tasks", taskId, taskAdapter) }
        catch (error: HttpException) { if (error.code() == 404) null else throw error }
    }

    fun observeApprovals(userId: String): Flow<List<ApprovalRequest>> = observe {
        fetchList("approvals", 200, approvalAdapter)
    }

    suspend fun createApproval(approval: ApprovalRequest): Result<String> = result {
        val id = "appr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        save("approvals", id, approval.copy(id = id), approvalAdapter)
        id
    }

    suspend fun decideApproval(
        approvalId: String,
        newStatus: String,
        decisionNote: String,
        userId: String? = null
    ): Result<Unit> = result {
        require(newStatus in setOf("approved", "rejected", "executed", "pending")) { "Invalid approval status." }
        val current = fetchOne("approvals", approvalId, approvalAdapter)
            ?: throw IllegalArgumentException("Approval request not found.")
        save("approvals", approvalId, current.copy(status = newStatus, decisionNote = decisionNote.take(500)), approvalAdapter)
    }

    fun observeChatMessages(userId: String): Flow<List<ChatMessage>> = observe {
        fetchList("chat_messages", 200, chatAdapter).sortedBy { it.createdAt?.time ?: 0L }
    }

    suspend fun addChatMessage(message: ChatMessage): Result<String> = result {
        val id = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        save("chat_messages", id, message.copy(id = id), chatAdapter)
        id
    }

    fun observeLogs(userId: String): Flow<List<OperationLog>> = observe {
        fetchList("operation_logs", 150, logAdapter)
    }

    suspend fun addLog(log: OperationLog): Result<String> = result {
        val id = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        save("operation_logs", id, log.copy(id = id), logAdapter)
        id
    }

    private suspend fun <T> result(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
