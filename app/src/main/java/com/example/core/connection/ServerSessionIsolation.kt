package com.example.core.connection

/**
 * Pure rules for preventing cross-server session and project leakage.
 * Tokens are bound to a server fingerprint (base URL + OAuth client id).
 */
object ServerSessionIsolation {
    fun mustClearSessionOnBind(
        previousFingerprint: String?,
        nextFingerprint: String
    ): Boolean {
        val prev = previousFingerprint?.trim().orEmpty()
        if (prev.isEmpty()) return false
        return prev != nextFingerprint
    }

    /**
     * A stored access token may only be used when the active server fingerprint
     * matches the fingerprint captured at last successful bind.
     */
    fun mayUseStoredSession(
        boundFingerprint: String?,
        activeFingerprint: String?
    ): Boolean {
        val bound = boundFingerprint?.trim().orEmpty()
        val active = activeFingerprint?.trim().orEmpty()
        if (bound.isEmpty() || active.isEmpty()) return false
        return bound == active
    }

    fun activeFingerprint(baseUrl: String, googleClientIdFromMeta: String): String =
        ServerConnectionRules.serverFingerprint(baseUrl, googleClientIdFromMeta)
}
