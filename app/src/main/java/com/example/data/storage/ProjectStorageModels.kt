package com.example.data.storage

/**
 * Multi-source project storage. Local is always available offline and without login.
 * Cloud providers use OAuth tokens stored only in SecretVault (never baked into the APK).
 */
enum class StorageProviderId(val wireName: String, val requiresNetwork: Boolean) {
    LOCAL("local", false),
    GOOGLE_DRIVE("google_drive", true),
    ONEDRIVE("onedrive", true),
    DROPBOX("dropbox", true),
    AI_IZ_BACKEND("ai_iz_backend", true);

    companion object {
        fun fromWire(name: String): StorageProviderId? =
            entries.firstOrNull { it.wireName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
    }
}

data class StoredProjectRef(
    val projectId: String,
    val projectName: String,
    val providerId: StorageProviderId,
    val revision: Long,
    val updatedAtEpochMs: Long,
    val description: String = "",
    val fileCount: Int = 0
)

data class ProjectVersionInfo(
    val projectId: String,
    val revision: Long,
    val createdAtEpochMs: Long,
    val note: String = ""
)

enum class ConflictPolicy {
    /** Keep both: write incoming as a conflict copy and retain current. */
    KEEP_BOTH,
    /** Accept incoming as the new current revision. */
    ACCEPT_INCOMING,
    /** Keep current and reject incoming. */
    KEEP_CURRENT
}

data class SaveOutcome(
    val projectId: String,
    val providerId: StorageProviderId,
    val revision: Long,
    val conflictCreated: Boolean = false,
    val conflictProjectId: String? = null
)

data class StorageProviderStatus(
    val providerId: StorageProviderId,
    val available: Boolean,
    val configured: Boolean,
    val message: String
)
