package com.example.data.storage

import com.example.domain.model.StudioProjectSpec

interface ProjectStorageProvider {
    val id: StorageProviderId

    suspend fun status(): StorageProviderStatus

    suspend fun listProjects(): Result<List<StoredProjectRef>>

    suspend fun loadProject(projectId: String): Result<StudioProjectSpec?>

    /**
     * @param expectedRevision null for first save; when set, mismatch triggers conflict policy.
     */
    suspend fun saveProject(
        project: StudioProjectSpec,
        expectedRevision: Long?,
        policy: ConflictPolicy = ConflictPolicy.KEEP_BOTH
    ): Result<SaveOutcome>

    suspend fun deleteProject(projectId: String): Result<Unit>

    suspend fun listVersions(projectId: String): Result<List<ProjectVersionInfo>>

    suspend fun restoreVersion(projectId: String, revision: Long): Result<StudioProjectSpec>
}
