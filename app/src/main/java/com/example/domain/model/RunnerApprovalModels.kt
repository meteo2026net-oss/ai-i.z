package com.example.domain.model

import com.squareup.moshi.Json
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
    val id: String = "",
    @Json(name = "action_type") val actionType: String = "",
    @Json(name = "target_resource") val targetResource: String = "",
    val operation: Map<String, Any>? = null,
    @Json(name = "operation_hash") val operationHash: String = "",
    val preview: String = "",
    val status: String = "",
    @Json(name = "decision_note") val decisionNote: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "consumed_at") val consumedAt: String? = null
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
