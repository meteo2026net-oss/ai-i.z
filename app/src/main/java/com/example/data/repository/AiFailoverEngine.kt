package com.example.data.repository

import com.example.core.security.AiEndpointConfig
import com.example.core.security.AiProviderType
import com.example.core.security.PlatformConfigStore
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class AiApiCallException(
    val httpCode: Int,
    val rawBody: String,
    message: String
) : Exception(message)

data class AiFailoverAlert(
    val exhaustedEndpointId: String,
    val exhaustedLabel: String,
    val exhaustedProvider: String,
    val switchedToLabel: String?,
    val reason: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class AiExecutionResult(
    val text: String,
    val usedEndpoint: AiEndpointConfig,
    val failoverAlerts: List<AiFailoverAlert> = emptyList()
)

data class AiFailureClassification(
    val isQuotaExhausted: Boolean,
    val isAuthFailure: Boolean,
    val arabicReason: String
)

interface AiRemoteTransport {
    suspend fun complete(
        endpoint: AiEndpointConfig,
        systemInstruction: String,
        userPrompt: String,
        expectJson: Boolean = false
    ): String
}

/**
 * Pure Kotlin Failover Pool Engine:
 * Iterates through the project's prioritized AI endpoints, detects HTTP 429 / quota exhaustion
 * / rate limits / insufficient credits, records exhausted state, emits failover alerts,
 * and seamlessly continues execution on the next available API/account.
 */
class AiFailoverEngine(
    private val configStore: PlatformConfigStore,
    private val transport: AiRemoteTransport
) {
    suspend fun executeWithFailover(
        projectId: String?,
        systemInstruction: String,
        userPrompt: String,
        expectJson: Boolean = false,
        preferredEndpointOverride: AiEndpointConfig? = null,
        excludedEndpointIds: Set<String> = emptySet(),
        onFailover: suspend (AiFailoverAlert) -> Unit = {}
    ): Result<AiExecutionResult> {
        val baseOrdered = configStore.resolveOrderedAiEndpoints(projectId)
            .filterNot { it.id in excludedEndpointIds }
            .toMutableList()
        if (preferredEndpointOverride != null &&
            preferredEndpointOverride.id !in excludedEndpointIds
        ) {
            baseOrdered.removeAll { it.id == preferredEndpointOverride.id }
            baseOrdered.add(0, preferredEndpointOverride)
        }
        if (baseOrdered.isEmpty()) {
            return Result.failure(
                IllegalStateException(
                    "لا توجد مفاتيح أو نماذج AI متاحة حالياً أو جميع الحصص مستنفدة مؤقتاً. سيتم الاستئناف تلقائياً عند توفر حصة أو إضافة بديل."
                )
            )
        }

        val alerts = mutableListOf<AiFailoverAlert>()
        var lastError: Throwable? = null

        for (index in baseOrdered.indices) {
            val endpoint = baseOrdered[index]
            try {
                val output = transport.complete(
                    endpoint = endpoint,
                    systemInstruction = systemInstruction,
                    userPrompt = userPrompt,
                    expectJson = expectJson
                )
                if (output.isBlank()) {
                    throw IllegalStateException("أرجع المزود (${endpoint.label}) استجابة فارغة.")
                }
                configStore.incrementEndpointSuccess(endpoint.id)
                return Result.success(
                    AiExecutionResult(
                        text = output,
                        usedEndpoint = endpoint,
                        failoverAlerts = alerts
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                val classified = classifyAiFailure(e)
                if (classified.isQuotaExhausted) {
                    configStore.markQuotaExhausted(endpoint.id, classified.arabicReason)
                } else {
                    configStore.markEndpointError(endpoint.id, classified.arabicReason)
                }
                val nextCandidate = baseOrdered.getOrNull(index + 1)
                val alert = AiFailoverAlert(
                    exhaustedEndpointId = endpoint.id,
                    exhaustedLabel = endpoint.label,
                    exhaustedProvider = AiProviderType.fromWireKey(endpoint.providerType).arabicLabel,
                    switchedToLabel = nextCandidate?.label,
                    reason = classified.arabicReason
                )
                alerts.add(alert)
                onFailover(alert)
            }
        }

        val detail = alerts.joinToString(" | ") { "${it.exhaustedLabel}: ${it.reason}" }
        return Result.failure(
            IllegalStateException(
                "تعذر إتمام الطلب بعد تجربة ${baseOrdered.size} مزود/حساب/نموذج AI. التفاصيل: $detail",
                lastError
            )
        )
    }

    companion object {
        fun classifyAiFailure(error: Throwable): AiFailureClassification {
            val httpCode = when (error) {
                is AiApiCallException -> error.httpCode
                is HttpException -> error.code()
                else -> -1
            }
            val rawText = buildString {
                append(error.message.orEmpty())
                if (error is AiApiCallException) {
                    append(' ')
                    append(error.rawBody)
                }
                append(' ')
                append(error.cause?.message.orEmpty())
            }.lowercase()

            val quotaSignal = httpCode == 429 || httpCode == 402 ||
                rawText.contains("resource_exhausted") ||
                rawText.contains("quota") ||
                rawText.contains("rate_limit") ||
                rawText.contains("rate limit") ||
                rawText.contains("insufficient_quota") ||
                rawText.contains("too many requests") ||
                rawText.contains("exceeded") ||
                rawText.contains("credit") ||
                rawText.contains("billing")

            val authSignal = httpCode == 401 ||
                (httpCode == 403 && !quotaSignal) ||
                rawText.contains("invalid api key") ||
                rawText.contains("api_key_invalid") ||
                rawText.contains("unauthorized")

            val modelUnavailableSignal = httpCode in listOf(404, 500, 502, 503, 504) ||
                rawText.contains("overloaded") ||
                rawText.contains("model_not_found") ||
                rawText.contains("not found") ||
                rawText.contains("unavailable") ||
                rawText.contains("timeout") ||
                rawText.contains("timed out")

            val reason = when {
                quotaSignal -> "انتهى الحد المسموح / الحصة (HTTP ${if (httpCode > 0) httpCode else 429})"
                authSignal -> "مفتاح API غير صالح أو مرفوض (HTTP ${if (httpCode > 0) httpCode else 401})"
                modelUnavailableSignal -> "توقف النموذج أو تعطل مؤقت (HTTP ${if (httpCode > 0) httpCode else 503})"
                else -> error.localizedMessage?.take(120) ?: "انقطع اتصال النموذج"
            }
            return AiFailureClassification(
                isQuotaExhausted = quotaSignal,
                isAuthFailure = authSignal,
                arabicReason = reason
            )
        }
    }
}
