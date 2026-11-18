package com.example.core.connection

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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

    fun parseMeta(json: String): ServerDiscoveryInfo? = runCatching {
        val root = JSONObject(json)
        val oauth = root.optJSONObject("oauth") ?: JSONObject()
        val caps = root.optJSONArray("capabilities")
        val capList = buildList {
            if (caps != null) {
                for (i in 0 until caps.length()) add(caps.optString(i))
            }
        }
        val modes = root.optJSONArray("authModes")
        val modeList = buildList {
            if (modes != null) {
                for (i in 0 until modes.length()) add(modes.optString(i))
            }
        }
        ServerDiscoveryInfo(
            service = root.optString("service"),
            apiVersion = root.optString("apiVersion"),
            protocol = root.optString("protocol"),
            minClientProtocol = root.optInt("minClientProtocol", 1),
            capabilities = capList,
            googleClientId = oauth.optString("googleClientId"),
            accessTokenAudience = oauth.optString("accessTokenAudience"),
            accessTokenIssuer = oauth.optString("accessTokenIssuer"),
            authModes = modeList
        )
    }.getOrNull()
}
