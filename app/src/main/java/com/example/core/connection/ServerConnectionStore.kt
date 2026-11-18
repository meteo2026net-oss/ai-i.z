package com.example.core.connection

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

/**
 * Non-secret server connection preferences.
 * Tokens and session material remain in [com.example.core.security.SecretVault].
 */
class ServerConnectionStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getMode(): ServerConnectionMode {
        val raw = prefs.getString(KEY_MODE, ServerConnectionMode.CENTRAL.name) ?: ServerConnectionMode.CENTRAL.name
        return runCatching { ServerConnectionMode.valueOf(raw) }.getOrDefault(ServerConnectionMode.CENTRAL)
    }

    fun setMode(mode: ServerConnectionMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun getCustomBaseUrl(): String = prefs.getString(KEY_CUSTOM_BASE_URL, "")?.trim().orEmpty()

    fun setCustomBaseUrl(url: String) {
        prefs.edit().putString(KEY_CUSTOM_BASE_URL, ServerConnectionRules.normalizeBaseUrl(url)).apply()
    }

    /** Build-time central default from CI/local Gradle properties. */
    fun getCentralBaseUrl(): String = BuildConfig.AI_IZ_API_BASE_URL.trim().trimEnd('/')

    /**
     * Active API origin used by repositories.
     * CENTRAL uses BuildConfig; CUSTOM uses the user-provided URL.
     */
    fun getActiveBaseUrl(): String {
        return when (getMode()) {
            ServerConnectionMode.CENTRAL -> getCentralBaseUrl()
            ServerConnectionMode.CUSTOM -> getCustomBaseUrl()
        }
    }

    fun getLastDiscoveryJson(): String = prefs.getString(KEY_LAST_META_JSON, "")?.orEmpty() ?: ""

    fun setLastDiscoveryJson(json: String) {
        prefs.edit().putString(KEY_LAST_META_JSON, json).apply()
    }

    fun getBoundFingerprint(): String = prefs.getString(KEY_BOUND_FINGERPRINT, "")?.orEmpty() ?: ""

    fun setBoundFingerprint(fp: String) {
        prefs.edit().putString(KEY_BOUND_FINGERPRINT, fp).apply()
    }

    fun clearConnectionState() {
        prefs.edit()
            .remove(KEY_LAST_META_JSON)
            .remove(KEY_BOUND_FINGERPRINT)
            .apply()
    }

    companion object {
        private const val PREFS = "ai_iz_server_connection"
        private const val KEY_MODE = "mode"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
        private const val KEY_LAST_META_JSON = "last_meta_json"
        private const val KEY_BOUND_FINGERPRINT = "bound_server_fingerprint"
    }
}
