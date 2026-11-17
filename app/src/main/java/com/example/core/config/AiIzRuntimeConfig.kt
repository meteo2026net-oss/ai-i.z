package com.example.core.config

/**
 * Build-time and runtime acceptance checks for AI I.Z identity connectivity.
 * Pure functions so unit tests do not depend on BuildConfig values from CI.
 */
object AiIzRuntimeConfig {
    fun isAcceptableApiBaseUrl(raw: String, debugBuild: Boolean): Boolean {
        val base = raw.trim()
        if (base.isEmpty()) return false
        if (base.startsWith("https://")) return true
        // Emulator loopback only — never allow plain http to arbitrary hosts.
        return debugBuild && base.startsWith("http://10.0.2.2")
    }

    fun isConfiguredGoogleWebClientId(raw: String): Boolean {
        val id = raw.trim()
        return id.isNotEmpty() && id.endsWith(".apps.googleusercontent.com")
    }

    fun isCloudSignInReady(apiBaseUrl: String, googleWebClientId: String, debugBuild: Boolean): Boolean =
        isAcceptableApiBaseUrl(apiBaseUrl, debugBuild) && isConfiguredGoogleWebClientId(googleWebClientId)
}
