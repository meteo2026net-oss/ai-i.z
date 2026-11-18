package com.example.core.connection

enum class ServerConnectionMode {
    CENTRAL,
    CUSTOM
}

data class ServerDiscoveryInfo(
    val service: String,
    val apiVersion: String,
    val protocol: String,
    val minClientProtocol: Int,
    val capabilities: List<String>,
    val googleClientId: String,
    val accessTokenAudience: String,
    val accessTokenIssuer: String,
    val authModes: List<String>
)

data class ConnectionTestResult(
    val ok: Boolean,
    val baseUrl: String,
    val discovery: ServerDiscoveryInfo? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

object ServerConnectionRules {
    const val CLIENT_PROTOCOL = 1
    const val REQUIRED_PROTOCOL = "ai-iz-identity/v1"
    val REQUIRED_CAPABILITIES = listOf("auth.google_oidc", "sessions.refresh")

    fun normalizeBaseUrl(raw: String): String {
        var u = raw.trim().trimEnd('/')
        return u
    }

    fun isAcceptableBaseUrl(raw: String, debugBuild: Boolean): Boolean {
        val base = normalizeBaseUrl(raw)
        if (base.isEmpty()) return false
        if (base.startsWith("https://")) return true
        return debugBuild && base.startsWith("http://10.0.2.2")
    }

    fun isCompatible(discovery: ServerDiscoveryInfo): Boolean {
        if (discovery.protocol != REQUIRED_PROTOCOL) return false
        if (discovery.minClientProtocol > CLIENT_PROTOCOL) return false
        if (!discovery.capabilities.containsAll(REQUIRED_CAPABILITIES)) return false
        if (discovery.googleClientId.isBlank()) return false
        return true
    }

    fun incompatibilityCode(discovery: ServerDiscoveryInfo): String? {
        if (discovery.protocol != REQUIRED_PROTOCOL) return "INCOMPATIBLE_PROTOCOL"
        if (discovery.minClientProtocol > CLIENT_PROTOCOL) return "CLIENT_TOO_OLD"
        if (!discovery.capabilities.containsAll(REQUIRED_CAPABILITIES)) return "MISSING_CAPABILITIES"
        if (discovery.googleClientId.isBlank()) return "OAUTH_CLIENT_MISSING"
        return null
    }

    /** Stable non-secret fingerprint used to detect server switches. */
    fun serverFingerprint(baseUrl: String, googleClientId: String): String {
        val key = normalizeBaseUrl(baseUrl).lowercase() + "|" + googleClientId.trim()
        return key.hashCode().toString(16)
    }
}
