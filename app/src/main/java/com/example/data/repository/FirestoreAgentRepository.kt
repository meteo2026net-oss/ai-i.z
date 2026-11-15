package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.domain.model.AgentTask
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.ChatMessage
import com.example.domain.model.OperationLog
import com.example.domain.model.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreAgentRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(explicitUserId: String? = null): String {
        return explicitUserId?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً قبل الوصول إلى Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeSettings(userId: String): Flow<UserSettings> = flow {
        val uid = requireUserId(userId)
        val docRef = userDoc(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: UserSettings(id = uid, userId = uid)
                }
                .catch { error ->
                    val fsError = unwrapFirestoreError(error)
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveSettings(settings: UserSettings): Result<Unit> = runCatching {
        val uid = requireUserId(settings.userId.ifBlank { null })
        val docRef = userDoc(uid)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(settings.toUpdateMap()).await()
            } else {
                docRef.set(settings.toCreateMap(uid)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeTasks(userId: String): Flow<List<AgentTask>> = flow {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("tasks")
        emitAll(
            colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AgentTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = unwrapFirestoreError(error)
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun createTask(task: AgentTask, explicitId: String? = null): Result<String> = runCatching {
        val uid = requireUserId(task.userId.ifBlank { null })
        val taskId = explicitId ?: "task_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = userDoc(uid).collection("tasks").document(taskId)
        try {
            docRef.set(task.toCreateMap(uid)).await()
            taskId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTask(task: AgentTask): Result<Unit> = runCatching {
        val uid = requireUserId(task.userId.ifBlank { null })
        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        val docRef = userDoc(uid).collection("tasks").document(task.id)
        try {
            docRef.update(task.toUpdateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun getTaskById(taskId: String, userId: String? = null): Result<AgentTask?> = runCatching {
        val uid = requireUserId(userId)
        val docRef = userDoc(uid).collection("tasks").document(taskId)
        try {
            docRef.get().await().toObject(
                AgentTask::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    fun observeApprovals(userId: String): Flow<List<ApprovalRequest>> = flow {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("approvals")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ApprovalRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = unwrapFirestoreError(error)
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun createApproval(approval: ApprovalRequest): Result<String> = runCatching {
        val uid = requireUserId(approval.userId.ifBlank { null })
        val apprId = "appr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = userDoc(uid).collection("approvals").document(apprId)
        try {
            docRef.set(approval.toCreateMap(uid)).await()
            apprId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun decideApproval(
        approvalId: String,
        newStatus: String,
        decisionNote: String,
        userId: String? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId(userId)
        val docRef = userDoc(uid).collection("approvals").document(approvalId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "decisionNote" to decisionNote.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeChatMessages(userId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("chat_messages")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = unwrapFirestoreError(error)
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun addChatMessage(message: ChatMessage): Result<String> = runCatching {
        val uid = requireUserId(message.userId.ifBlank { null })
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = userDoc(uid).collection("chat_messages").document(msgId)
        try {
            docRef.set(message.toCreateMap(uid)).await()
            msgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeLogs(userId: String): Flow<List<OperationLog>> = flow {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("operation_logs")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(150)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        OperationLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = unwrapFirestoreError(error)
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun addLog(log: OperationLog): Result<String> = runCatching {
        val uid = requireUserId(log.userId.ifBlank { null })
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = userDoc(uid).collection("operation_logs").document(logId)
        try {
            docRef.set(log.toCreateMap(uid)).await()
            logId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }
}
