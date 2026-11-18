package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.StudioProjectSpec
import com.example.domain.model.StudioProjectSummary

/**
 * Unified storage engine: local-first library, optional cloud providers, per-project location.
 * Backend/PostgreSQL are never required to retain projects.
 */
class ProjectStorageEngine(
    context: Context,
    vault: com.example.core.security.SecretStorage,
    private val local: ProjectStorageProvider = LocalProjectStorageProvider(context),
    private val googleDrive: ProjectStorageProvider = GoogleDriveStorageProvider(vault),
    private val oneDrive: ProjectStorageProvider = OneDriveStorageProvider(vault),
    private val dropbox: ProjectStorageProvider = DropboxStorageProvider(vault)
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val providers: Map<StorageProviderId, ProjectStorageProvider> = mapOf(
        StorageProviderId.LOCAL to local,
        StorageProviderId.GOOGLE_DRIVE to googleDrive,
        StorageProviderId.ONEDRIVE to oneDrive,
        StorageProviderId.DROPBOX to dropbox
    )

    fun defaultProvider(): StorageProviderId {
        val raw = prefs.getString(KEY_DEFAULT_PROVIDER, StorageProviderId.LOCAL.wireName)
        return StorageProviderId.fromWire(raw ?: "") ?: StorageProviderId.LOCAL
    }

    fun setDefaultProvider(id: StorageProviderId) {
        // Only LOCAL is always usable offline; cloud remains selectable once linked.
        prefs.edit().putString(KEY_DEFAULT_PROVIDER, id.wireName).apply()
    }

    fun providerForProject(projectId: String): StorageProviderId {
        val raw = prefs.getString(KEY_PROJECT_PROVIDER_PREFIX + projectId, null)
        return StorageProviderId.fromWire(raw ?: "") ?: defaultProvider()
    }

    fun setProviderForProject(projectId: String, id: StorageProviderId) {
        prefs.edit().putString(KEY_PROJECT_PROVIDER_PREFIX + projectId, id.wireName).apply()
    }

    suspend fun providerStatuses(): List<StorageProviderStatus> =
        providers.values.map { it.status() }

    suspend fun save(
        project: StudioProjectSpec,
        providerId: StorageProviderId? = null,
        expectedRevision: Long? = null,
        policy: ConflictPolicy = ConflictPolicy.KEEP_BOTH
    ): Result<SaveOutcome> {
        val target = providerId ?: providerForProject(project.id)
        val provider = providers[target] ?: local
        val outcome = provider.saveProject(project, expectedRevision, policy)
        outcome.onSuccess { setProviderForProject(project.id, target) }
        // Always mirror a safety copy locally when saving to cloud succeeds in the future.
        if (target != StorageProviderId.LOCAL && outcome.isSuccess) {
            local.saveProject(project, expectedRevision = null, policy = ConflictPolicy.ACCEPT_INCOMING)
        }
        return outcome
    }

    suspend fun listAll(): Result<List<StoredProjectRef>> {
        // Local is authoritative for offline; cloud lists only when linked (else empty failure ignored).
        val localList = local.listProjects().getOrElse { emptyList() }
        return Result.success(localList)
    }

    suspend fun load(projectId: String, providerId: StorageProviderId? = null): Result<StudioProjectSpec?> {
        val target = providerId ?: providerForProject(projectId)
        val primary = providers[target] ?: local
        val loaded = primary.loadProject(projectId)
        if (loaded.isSuccess && loaded.getOrNull() != null) return loaded
        if (target != StorageProviderId.LOCAL) {
            return local.loadProject(projectId)
        }
        return loaded
    }

    suspend fun delete(projectId: String): Result<Unit> {
        val target = providerForProject(projectId)
        val primary = providers[target] ?: local
        primary.deleteProject(projectId)
        if (target != StorageProviderId.LOCAL) {
            local.deleteProject(projectId)
        }
        prefs.edit().remove(KEY_PROJECT_PROVIDER_PREFIX + projectId).apply()
        return Result.success(Unit)
    }

    suspend fun listVersions(projectId: String): Result<List<ProjectVersionInfo>> {
        val target = providerForProject(projectId)
        return (providers[target] ?: local).listVersions(projectId)
    }

    suspend fun restoreVersion(projectId: String, revision: Long): Result<StudioProjectSpec> {
        val target = providerForProject(projectId)
        return (providers[target] ?: local).restoreVersion(projectId, revision)
    }

    fun toSummary(ref: StoredProjectRef, environmentName: String = "MOBILE_ANDROID", languageName: String = "KOTLIN"): StudioProjectSummary {
        // Lightweight bridge for existing UI rows; full enum resolution happens on open.
        return StudioProjectSummary(
            id = ref.projectId,
            projectName = ref.projectName,
            environment = com.example.domain.model.DevelopmentEnvironment.MOBILE_ANDROID,
            primaryLanguage = com.example.domain.model.ProgrammingLanguage.KOTLIN,
            description = ref.description.ifBlank { "Local revision ${ref.revision}" },
            fileCount = ref.fileCount
        )
    }

    companion object {
        private const val PREFS = "ai_iz_project_storage"
        private const val KEY_DEFAULT_PROVIDER = "default_provider"
        private const val KEY_PROJECT_PROVIDER_PREFIX = "project_provider_"
    }
}
