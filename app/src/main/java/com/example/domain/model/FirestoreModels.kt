package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val activeRepo: String = "gfa437926-tech/metra2027",
    val activeBranch: String = "main",
    val aiProvider: String = "gemini",
    val aiModel: String = "gemini-3.8-flash",
    val backendUrl: String = "",
    val darkTheme: Boolean = true,
    val autoExecuteTier1: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "activeRepo" to activeRepo.take(128),
        "activeBranch" to activeBranch.take(128),
        "aiProvider" to if (aiProvider == "openai") "openai" else "gemini",
        "aiModel" to aiModel.ifBlank { "gemini-3.8-flash" }.take(64),
        "backendUrl" to backendUrl.take(300),
        "darkTheme" to darkTheme,
        "autoExecuteTier1" to autoExecuteTier1,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "activeRepo" to activeRepo.take(128),
        "activeBranch" to activeBranch.take(128),
        "aiProvider" to if (aiProvider == "openai") "openai" else "gemini",
        "aiModel" to aiModel.ifBlank { "gemini-3.8-flash" }.take(64),
        "backendUrl" to backendUrl.take(300),
        "darkTheme" to darkTheme,
        "autoExecuteTier1" to autoExecuteTier1,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class AgentTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val repo: String = "gfa437926-tech/metra2027",
    val branch: String = "main",
    val title: String = "",
    val prompt: String = "",
    val status: String = "queued",
    val currentStepIndex: Long = 0L,
    val totalSteps: Long = 0L,
    val progressPercent: Long = 0L,
    val lastCompletedStep: String = "",
    val lastResultSummary: String = "",
    val lastError: String = "",
    val modifiedFiles: List<String> = emptyList(),
    val nextSteps: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "repo" to repo.take(128),
        "branch" to branch.take(128),
        "title" to title.ifBlank { "مهمة METRA" }.take(200),
        "prompt" to prompt.ifBlank { "فحص المشروع" }.take(2000),
        "status" to status,
        "currentStepIndex" to currentStepIndex.coerceAtLeast(0),
        "totalSteps" to totalSteps.coerceAtLeast(0),
        "progressPercent" to progressPercent.coerceIn(0, 100),
        "lastCompletedStep" to lastCompletedStep.take(300),
        "lastResultSummary" to lastResultSummary.take(2000),
        "lastError" to lastError.take(1000),
        "modifiedFiles" to modifiedFiles.take(30),
        "nextSteps" to nextSteps.take(30),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "repo" to repo.take(128),
        "branch" to branch.take(128),
        "title" to title.ifBlank { "مهمة METRA" }.take(200),
        "prompt" to prompt.ifBlank { "فحص المشروع" }.take(2000),
        "status" to status,
        "currentStepIndex" to currentStepIndex.coerceAtLeast(0),
        "totalSteps" to totalSteps.coerceAtLeast(0),
        "progressPercent" to progressPercent.coerceIn(0, 100),
        "lastCompletedStep" to lastCompletedStep.take(300),
        "lastResultSummary" to lastResultSummary.take(2000),
        "lastError" to lastError.take(1000),
        "modifiedFiles" to modifiedFiles.take(30),
        "nextSteps" to nextSteps.take(30),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ApprovalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val taskId: String = "",
    val tier: Long = 2L,
    val actionType: String = "git_push",
    val title: String = "",
    val description: String = "",
    val payloadPreview: String = "",
    val targetResource: String = "",
    val status: String = "pending",
    val decisionNote: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "taskId" to taskId.take(128),
        "tier" to if (tier == 3L) 3L else 2L,
        "actionType" to actionType,
        "title" to title.ifBlank { "طلب موافقة" }.take(200),
        "description" to description.ifBlank { "موافقة مطلوبة قبل التنفيذ" }.take(2000),
        "payloadPreview" to payloadPreview.take(8000),
        "targetResource" to targetResource.take(300),
        "status" to "pending",
        "decisionNote" to decisionNote.take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val content: String = "",
    val repoContext: String = "",
    val linkedTaskId: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "role" to if (role in listOf("user", "assistant", "system")) role else "user",
        "content" to content.ifBlank { "..." }.take(12000),
        "repoContext" to repoContext.take(128),
        "linkedTaskId" to linkedTaskId.take(128),
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class OperationLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "agent",
    val tier: Long = 1L,
    val action: String = "",
    val details: String = "",
    val outcome: String = "success",
    val repo: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "category" to category,
        "tier" to tier.coerceIn(1L, 3L),
        "action" to action.ifBlank { "عملية" }.take(200),
        "details" to details.take(3000),
        "outcome" to outcome,
        "repo" to repo.take(128),
        "createdAt" to FieldValue.serverTimestamp()
    )
}
