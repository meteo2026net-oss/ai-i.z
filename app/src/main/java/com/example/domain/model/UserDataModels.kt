package com.example.domain.model

import java.util.Date

data class UserSettings(
    val id: String = "",
    val userId: String = "",
    val activeRepo: String = "gfa437926-tech/metra2027",
    val activeBranch: String = "main",
    val aiProvider: String = "gemini",
    val aiModel: String = "gemini-3.8-flash",
    val backendUrl: String = "",
    val darkTheme: Boolean = true,
    val autoExecuteTier1: Boolean = true,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class AgentTask(
    val id: String = "",
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
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class ApprovalRequest(
    val id: String = "",
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
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val operationPayload: Map<String, Any> = emptyMap(),
    val operationHash: String = "",
    val serverOwned: Boolean = false
)

data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val content: String = "",
    val repoContext: String = "",
    val linkedTaskId: String = "",
    val createdAt: Date? = null
)

data class OperationLog(
    val id: String = "",
    val userId: String = "",
    val category: String = "agent",
    val tier: Long = 1L,
    val action: String = "",
    val details: String = "",
    val outcome: String = "success",
    val repo: String = "",
    val createdAt: Date? = null
)
