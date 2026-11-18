package com.example.data.storage

import com.example.domain.model.StudioProjectSpec

/** Pure JVM fake for unit tests (no Android JSONObject). */
class InMemoryProjectStorageProvider(
    override val id: StorageProviderId = StorageProviderId.LOCAL
) : ProjectStorageProvider {
    private data class Entry(var revision: Long, var project: StudioProjectSpec, val versions: MutableMap<Long, StudioProjectSpec> = mutableMapOf())
    private val data = linkedMapOf<String, Entry>()

    override suspend fun status() = StorageProviderStatus(id, true, true, "memory")

    override suspend fun listProjects(): Result<List<StoredProjectRef>> = Result.success(
        data.values.map {
            StoredProjectRef(
                projectId = it.project.id,
                projectName = it.project.projectName,
                providerId = id,
                revision = it.revision,
                updatedAtEpochMs = it.revision,
                description = it.project.description,
                fileCount = it.project.files.size
            )
        }
    )

    override suspend fun loadProject(projectId: String): Result<StudioProjectSpec?> =
        Result.success(data[projectId]?.project)

    override suspend fun saveProject(
        project: StudioProjectSpec,
        expectedRevision: Long?,
        policy: ConflictPolicy
    ): Result<SaveOutcome> {
        val existing = data[project.id]
        if (existing != null && expectedRevision != null && expectedRevision != existing.revision) {
            when (policy) {
                ConflictPolicy.KEEP_CURRENT -> return Result.success(
                    SaveOutcome(project.id, id, existing.revision, conflictCreated = true)
                )
                ConflictPolicy.KEEP_BOTH -> {
                    val conflictId = "${project.id}__conflict"
                    val conflict = project.copy(id = conflictId, projectName = "${project.projectName} (conflict)")
                    data[conflictId] = Entry(1, conflict, mutableMapOf(1L to conflict))
                    return Result.success(
                        SaveOutcome(project.id, id, existing.revision, true, conflictId)
                    )
                }
                ConflictPolicy.ACCEPT_INCOMING -> Unit
            }
        }
        val next = (existing?.revision ?: 0L) + 1L
        val versions = existing?.versions ?: mutableMapOf()
        versions[next] = project
        data[project.id] = Entry(next, project, versions)
        return Result.success(SaveOutcome(project.id, id, next))
    }

    override suspend fun deleteProject(projectId: String): Result<Unit> {
        data.remove(projectId)
        return Result.success(Unit)
    }

    override suspend fun listVersions(projectId: String): Result<List<ProjectVersionInfo>> {
        val entry = data[projectId] ?: return Result.success(emptyList())
        return Result.success(entry.versions.keys.sortedDescending().map {
            ProjectVersionInfo(projectId, it, it)
        })
    }

    override suspend fun restoreVersion(projectId: String, revision: Long): Result<StudioProjectSpec> {
        val entry = data[projectId] ?: return Result.failure(IllegalStateException("missing"))
        val version = entry.versions[revision] ?: return Result.failure(IllegalStateException("missing version"))
        saveProject(version.copy(id = projectId), expectedRevision = null).getOrThrow()
        return Result.success(version.copy(id = projectId))
    }
}
