package com.example.core.connection

import com.example.BuildConfig
import com.example.core.security.SecretVault

/**
 * Orchestrates discovery, binding, and safe server switching.
 * Changing servers always clears session tokens so sessions never cross hosts.
 */
class ServerConnectionManager(
    private val store: ServerConnectionStore,
    private val vault: SecretVault,
    private val discoveryClient: ServerDiscoveryClient = ServerDiscoveryClient()
) {
    fun activeBaseUrl(): String = store.getActiveBaseUrl()

    fun testActive(debugBuild: Boolean = BuildConfig.DEBUG): ConnectionTestResult {
        val url = store.getActiveBaseUrl()
        return discoveryClient.probe(url, debugBuild)
    }

    fun testUrl(url: String, debugBuild: Boolean = BuildConfig.DEBUG): ConnectionTestResult =
        discoveryClient.probe(url, debugBuild)

    /**
     * Apply a successful probe: persist non-secret prefs and bind fingerprint.
     * Clears vault auth material when the bound server changes.
     */
    fun applyConnection(
        mode: ServerConnectionMode,
        customUrl: String?,
        result: ConnectionTestResult
    ): Result<Unit> = runCatching {
        require(result.ok && result.discovery != null) {
            result.errorMessage ?: "Connection test did not succeed"
        }
        val discovery = result.discovery
        val base = result.baseUrl
        val fp = ServerConnectionRules.serverFingerprint(base, discovery.googleClientId)
        val previous = store.getBoundFingerprint()
        if (previous.isNotBlank() && previous != fp) {
            clearSessionSecrets()
        }
        store.setMode(mode)
        if (mode == ServerConnectionMode.CUSTOM && customUrl != null) {
            store.setCustomBaseUrl(customUrl)
        }
        store.setLastDiscoveryJson(serializeDiscovery(discovery))
        store.setBoundFingerprint(fp)
    }

    fun disconnect() {
        clearSessionSecrets()
        store.clearConnectionState()
        store.setMode(ServerConnectionMode.CENTRAL)
    }

    fun switchRequiresReauth(nextBaseUrl: String, nextClientId: String): Boolean {
        val next = ServerConnectionRules.serverFingerprint(nextBaseUrl, nextClientId)
        val current = store.getBoundFingerprint()
        return current.isNotBlank() && current != next
    }

    private fun clearSessionSecrets() {
        vault.putSecret(SecretVault.KEY_AUTH_ACCESS_TOKEN, "")
        vault.putSecret(SecretVault.KEY_AUTH_REFRESH_TOKEN, "")
        vault.putSecret(SecretVault.KEY_AUTH_USER_ID, "")
        vault.putSecret(SecretVault.KEY_AUTH_EMAIL, "")
        vault.putSecret(SecretVault.KEY_AUTH_DISPLAY_NAME, "")
    }

    private fun serializeDiscovery(d: ServerDiscoveryInfo): String {
        return """{"service":"${d.service}","apiVersion":"${d.apiVersion}","protocol":"${d.protocol}","minClientProtocol":${d.minClientProtocol},"googleClientId":"${d.googleClientId}","accessTokenAudience":"${d.accessTokenAudience}","accessTokenIssuer":"${d.accessTokenIssuer}"}"""
    }
}
