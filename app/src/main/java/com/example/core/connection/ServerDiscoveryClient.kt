package com.example.core.connection

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ServerDiscoveryClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    fun probe(baseUrl: String, debugBuild: Boolean): ConnectionTestResult {
        val normalized = ServerConnectionRules.normalizeBaseUrl(baseUrl)
        if (!ServerConnectionRules.isAcceptableBaseUrl(normalized, debugBuild)) {
            return ConnectionTestResult(
                ok = false,
                baseUrl = normalized,
                errorCode = "INVALID_BASE_URL",
                errorMessage = "API base URL must use HTTPS (or emulator http://10.0.2.2 in debug)."
            )
        }
        val metaUrl = if (normalized.endsWith("/")) normalized + "api/meta" else "$normalized/api/meta"
        return try {
            val request = Request.Builder().url(metaUrl).get().header("Accept", "application/json").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ConnectionTestResult(
                        ok = false,
                        baseUrl = normalized,
                        errorCode = "HTTP_${response.code}",
                        errorMessage = "Discovery failed with HTTP ${response.code}"
                    )
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    return ConnectionTestResult(
                        ok = false,
                        baseUrl = normalized,
                        errorCode = "EMPTY_META",
                        errorMessage = "Server returned an empty discovery document"
                    )
                }
                val discovery = parseMeta(body)
                    ?: return ConnectionTestResult(
                        ok = false,
                        baseUrl = normalized,
                        errorCode = "MALFORMED_META",
                        errorMessage = "Server discovery document could not be parsed"
                    )
                val code = ServerConnectionRules.incompatibilityCode(discovery)
                if (code != null) {
                    return ConnectionTestResult(
                        ok = false,
                        baseUrl = normalized,
                        discovery = discovery,
                        errorCode = code,
                        errorMessage = "Server is not compatible with this app build ($code)"
                    )
                }
                ConnectionTestResult(ok = true, baseUrl = normalized, discovery = discovery)
            }
        } catch (e: Exception) {
            ConnectionTestResult(
                ok = false,
                baseUrl = normalized,
                errorCode = "NETWORK_ERROR",
                errorMessage = e.message ?: "Network error while reaching the server"
            )
        }
    }

    /** JVM-safe JSON field extraction (no android.org.json). */
    fun parseMeta(json: String): ServerDiscoveryInfo? = runCatching {
        fun stringField(key: String): String {
            val re = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
            return re.find(json)?.groupValues?.getOrNull(1).orEmpty()
        }
        fun intField(key: String, default: Int): Int {
            val re = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
            return re.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: default
        }
        fun stringArray(key: String): List<String> {
            val re = Regex("\"$key\"\\s*:\\s*\\[(.*?)]", setOf(RegexOption.DOT_MATCHES_ALL))
            val block = re.find(json)?.groupValues?.getOrNull(1) ?: return emptyList()
            return Regex("\"([^\"]+)\"").findAll(block).map { it.groupValues[1] }.toList()
        }
        ServerDiscoveryInfo(
            service = stringField("service"),
            apiVersion = stringField("apiVersion"),
            protocol = stringField("protocol"),
            minClientProtocol = intField("minClientProtocol", 1),
            capabilities = stringArray("capabilities"),
            googleClientId = stringField("googleClientId"),
            accessTokenAudience = stringField("accessTokenAudience"),
            accessTokenIssuer = stringField("accessTokenIssuer"),
            authModes = stringArray("authModes")
        ).takeIf { it.service.isNotBlank() && it.protocol.isNotBlank() }
    }.getOrNull()
}
