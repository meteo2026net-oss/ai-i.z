package com.example.data.storage

import com.example.core.security.SecretStorage
import com.example.domain.model.StudioProjectSpec

/**
 * Base for official OAuth cloud file providers.
 * Tokens live only in [SecretStorage]. No client secrets are embedded in the APK.
 * Network calls are not simulated as success without a real token and endpoint.
 */
abstract class OAuthCloudStorageProvider(
    private val vault: SecretStorage,
    private val accessTokenKey: String,
    private val refreshTokenKey: String,
    override val id: StorageProviderId
) : ProjectStorageProvider {

    protected fun hasAccessToken(): Boolean = vault.getSecret(accessTokenKey).isNotBlank()

    fun clearTokens() {
        vault.putSecret(accessTokenKey, "")
        vault.putSecret(refreshTokenKey, "")
    }

    fun storeTokens(accessToken: String, refreshToken: String?) {
        require(accessToken.isNotBlank()) { "Access token required" }
        vault.putSecret(accessTokenKey, accessToken)
        if (refreshToken != null) vault.putSecret(refreshTokenKey, refreshToken)
    }

    override suspend fun status(): StorageProviderStatus {
        val configured = hasAccessToken()
        return StorageProviderStatus(
            providerId = id,
            available = true,
            configured = configured,
            message = if (configured) {
                "OAuth token present — live cloud I/O requires validated API calls"
            } else {
                "Not linked. Complete official OAuth to enable ${id.wireName}."
            }
        )
    }

    override suspend fun listProjects(): Result<List<StoredProjectRef>> =
        notLinkedOr("Cloud list requires a linked ${id.wireName} account and live API access.")

    override suspend fun loadProject(projectId: String): Result<StudioProjectSpec?> =
        notLinkedOr("Cloud load requires a linked ${id.wireName} account and live API access.")

    override suspend fun saveProject(
        project: StudioProjectSpec,
        expectedRevision: Long?,
        policy: ConflictPolicy
    ): Result<SaveOutcome> =
        notLinkedOr("Cloud save requires a linked ${id.wireName} account and live API access.")

    override suspend fun deleteProject(projectId: String): Result<Unit> =
        notLinkedOr("Cloud delete requires a linked ${id.wireName} account and live API access.")

    override suspend fun listVersions(projectId: String): Result<List<ProjectVersionInfo>> =
        notLinkedOr("Cloud versions require a linked ${id.wireName} account and live API access.")

    override suspend fun restoreVersion(projectId: String, revision: Long): Result<StudioProjectSpec> =
        notLinkedOr("Cloud restore requires a linked ${id.wireName} account and live API access.")

    private fun <T> notLinkedOr(message: String): Result<T> {
        if (!hasAccessToken()) {
            return Result.failure(IllegalStateException("PROVIDER_NOT_LINKED: $message"))
        }
        // Token present but live HTTP integration is not asserted without a real account test.
        return Result.failure(
            IllegalStateException(
                "PROVIDER_NOT_VERIFIED_LIVE: ${id.wireName} OAuth token is stored, " +
                    "but cloud file API integration must be verified with a real account. $message"
            )
        )
    }

    companion object {
        const val KEY_GOOGLE_DRIVE_ACCESS = "storage_google_drive_access_token"
        const val KEY_GOOGLE_DRIVE_REFRESH = "storage_google_drive_refresh_token"
        const val KEY_ONEDRIVE_ACCESS = "storage_onedrive_access_token"
        const val KEY_ONEDRIVE_REFRESH = "storage_onedrive_refresh_token"
        const val KEY_DROPBOX_ACCESS = "storage_dropbox_access_token"
        const val KEY_DROPBOX_REFRESH = "storage_dropbox_refresh_token"
    }
}

class GoogleDriveStorageProvider(vault: SecretStorage) : OAuthCloudStorageProvider(
    vault,
    KEY_GOOGLE_DRIVE_ACCESS,
    KEY_GOOGLE_DRIVE_REFRESH,
    StorageProviderId.GOOGLE_DRIVE
)

class OneDriveStorageProvider(vault: SecretStorage) : OAuthCloudStorageProvider(
    vault,
    KEY_ONEDRIVE_ACCESS,
    KEY_ONEDRIVE_REFRESH,
    StorageProviderId.ONEDRIVE
)

class DropboxStorageProvider(vault: SecretStorage) : OAuthCloudStorageProvider(
    vault,
    KEY_DROPBOX_ACCESS,
    KEY_DROPBOX_REFRESH,
    StorageProviderId.DROPBOX
)
