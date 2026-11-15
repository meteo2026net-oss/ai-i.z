package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RunnerApprovalOperation(
    val actionType: String,
    val targetResource: String,
    val operation: Map<String, Any>,
    val preview: String
)

@JsonClass(generateAdapter = true)
data class RunnerApprovalDecision(
    val decision: String,
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class RunnerApprovalConsumeRequest(
    val actionType: String,
    val targetResource: String,
    val operationHash: String
)

@JsonClass(generateAdapter = true)
data class RunnerApprovalReport(
    val outcome: String,
    val note: String = ""
)

/**
 * Server-owned approval metadata. The server's operationHash binds approval
 * to the exact immutable operation; client reports are never execution proof.
 */
@JsonClass(generateAdapter = true)
data class RunnerApprovalRecord(
    val id: String,
    val actionType: String,
    val targetResource: String,
    val operation: Map<String, Any>? = null,
    val operationHash: String,
    val preview: String,
    val status: String,
    val decisionNote: String = "",
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val consumedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RunnerApprovalEnvelope(
    val approval: RunnerApprovalRecord
)

@JsonClass(generateAdapter = true)
data class RunnerApprovalListEnvelope(
    val approvals: List<RunnerApprovalRecord>
)

@JsonClass(generateAdapter = true)
data class RunnerApprovalConsumeEnvelope(
    val approval: RunnerApprovalRecord
)

@JsonClass(generateAdapter = true)
data class RunnerApprovalReportEnvelope(
    val approval: RunnerApprovalRecord,
    val executionVerified: Boolean = false
)
