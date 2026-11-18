package com.example.data.storage

import android.content.Context
import com.example.domain.model.DevelopmentEnvironment
import com.example.domain.model.ProgrammingLanguage
import com.example.domain.model.StudioFileItem
import com.example.domain.model.StudioProjectSpec
import com.example.domain.model.AiModelRecommendation
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Complete offline project library under app internal storage.
 * Does not require network, login, PostgreSQL, or Backend.
 */
class LocalProjectStorageProvider(
    context: Context,
    rootDir: File? = null
) : ProjectStorageProvider {

    override val id: StorageProviderId = StorageProviderId.LOCAL

    private val root: File = (rootDir ?: File(context.applicationContext.filesDir, "project_storage/local")).apply { mkdirs() }

    override suspend fun status(): StorageProviderStatus =
        StorageProviderStatus(id, available = true, configured = true, message = "Local offline storage")

    override suspend fun listProjects(): Result<List<StoredProjectRef>> = runCatching {
        root.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir -> readManifest(dir.name) }
            ?.sortedByDescending { it.updatedAtEpochMs }
            ?: emptyList()
    }

    override suspend fun loadProject(projectId: String): Result<StudioProjectSpec?> = runCatching {
        val file = File(projectDir(projectId), CURRENT_FILE)
        if (!file.exists()) return@runCatching null
        decodeProject(file.readText(Charsets.UTF_8))
    }

    override suspend fun saveProject(
        project: StudioProjectSpec,
        expectedRevision: Long?,
        policy: ConflictPolicy
    ): Result<SaveOutcome> = runCatching {
        require(project.id.matches(ID_REGEX)) { "Invalid project id" }
        val dir = projectDir(project.id).apply { mkdirs() }
        val manifestFile = File(dir, MANIFEST_FILE)
        val currentRev = if (manifestFile.exists()) {
            JSONObject(manifestFile.readText()).optLong("revision", 0L)
        } else {
            0L
        }
        if (expectedRevision != null && currentRev != 0L && expectedRevision != currentRev) {
            when (policy) {
                ConflictPolicy.KEEP_CURRENT -> {
                    return@runCatching SaveOutcome(project.id, id, currentRev, conflictCreated = true)
                }
                ConflictPolicy.ACCEPT_INCOMING -> {
                    // fall through and overwrite
                }
                ConflictPolicy.KEEP_BOTH -> {
                    val conflictId = "${project.id}__conflict_${System.currentTimeMillis()}"
                    val conflictProject = project.copy(id = conflictId, projectName = "${project.projectName} (conflict)")
                    writeProject(conflictProject, expectedRevision = null)
                    return@runCatching SaveOutcome(
                        projectId = project.id,
                        providerId = id,
                        revision = currentRev,
                        conflictCreated = true,
                        conflictProjectId = conflictId
                    )
                }
            }
        }
        writeProject(project, expectedRevision = currentRev)
    }

    override suspend fun deleteProject(projectId: String): Result<Unit> = runCatching {
        val dir = projectDir(projectId)
        if (dir.exists()) dir.deleteRecursively()
        Unit
    }

    override suspend fun listVersions(projectId: String): Result<List<ProjectVersionInfo>> = runCatching {
        val versionsDir = File(projectDir(projectId), VERSIONS_DIR)
        versionsDir.listFiles()
            ?.filter { it.name.startsWith("v") && it.name.endsWith(".json") }
            ?.mapNotNull { f ->
                val rev = f.name.removePrefix("v").removeSuffix(".json").toLongOrNull() ?: return@mapNotNull null
                ProjectVersionInfo(projectId, rev, f.lastModified())
            }
            ?.sortedByDescending { it.revision }
            ?: emptyList()
    }

    override suspend fun restoreVersion(projectId: String, revision: Long): Result<StudioProjectSpec> = runCatching {
        val versionFile = File(File(projectDir(projectId), VERSIONS_DIR), "v$revision.json")
        require(versionFile.exists()) { "Version $revision not found" }
        val restored = decodeProject(versionFile.readText(Charsets.UTF_8)).copy(id = projectId)
        writeProject(restored, expectedRevision = null)
        restored
    }

    private fun writeProject(project: StudioProjectSpec, expectedRevision: Long?): SaveOutcome {
        val dir = projectDir(project.id).apply { mkdirs() }
        val versionsDir = File(dir, VERSIONS_DIR).apply { mkdirs() }
        val manifestFile = File(dir, MANIFEST_FILE)
        val currentRev = if (manifestFile.exists()) {
            JSONObject(manifestFile.readText()).optLong("revision", 0L)
        } else {
            0L
        }
        val nextRev = currentRev + 1L
        val payload = encodeProject(project)
        File(dir, CURRENT_FILE).writeText(payload, Charsets.UTF_8)
        File(versionsDir, "v$nextRev.json").writeText(payload, Charsets.UTF_8)
        // Cap retained versions
        versionsDir.listFiles()
            ?.sortedByDescending { it.name }
            ?.drop(MAX_VERSIONS)
            ?.forEach { it.delete() }
        val now = System.currentTimeMillis()
        val manifest = JSONObject()
            .put("projectId", project.id)
            .put("projectName", project.projectName)
            .put("revision", nextRev)
            .put("updatedAtEpochMs", now)
            .put("description", project.description.take(500))
            .put("fileCount", project.files.size)
            .put("provider", id.wireName)
        manifestFile.writeText(manifest.toString())
        return SaveOutcome(project.id, id, nextRev)
    }

    private fun readManifest(projectId: String): StoredProjectRef? {
        val file = File(projectDir(projectId), MANIFEST_FILE)
        if (!file.exists()) return null
        val o = JSONObject(file.readText())
        return StoredProjectRef(
            projectId = o.optString("projectId", projectId),
            projectName = o.optString("projectName"),
            providerId = StorageProviderId.LOCAL,
            revision = o.optLong("revision", 1L),
            updatedAtEpochMs = o.optLong("updatedAtEpochMs", file.lastModified()),
            description = o.optString("description"),
            fileCount = o.optInt("fileCount", 0)
        )
    }

    private fun projectDir(projectId: String): File = File(root, projectId)

    companion object {
        private val ID_REGEX = Regex("^[A-Za-z0-9_.:-]{1,128}$")
        private const val CURRENT_FILE = "current.json"
        private const val MANIFEST_FILE = "manifest.json"
        private const val VERSIONS_DIR = "versions"
        private const val MAX_VERSIONS = 30

        fun encodeProject(project: StudioProjectSpec): String {
            val files = JSONArray()
            project.files.forEach { f ->
                files.put(
                    JSONObject()
                        .put("path", f.path)
                        .put("language", f.language.name)
                        .put("content", f.content)
                        .put("purpose", f.purpose)
                        .put("isEntrypoint", f.isEntrypoint)
                )
            }
            val model = JSONObject()
                .put("modelId", project.recommendedModel.modelId)
                .put("modelName", project.recommendedModel.modelName)
                .put("provider", project.recommendedModel.provider)
                .put("matchScore", project.recommendedModel.matchScore)
                .put("strengthBadge", project.recommendedModel.strengthBadge)
                .put("contextWindow", project.recommendedModel.contextWindow)
                .put("teamRole", project.recommendedModel.teamRole)
                .put("rationale", project.recommendedModel.rationale)
                .put("isPrimaryPick", project.recommendedModel.isPrimaryPick)
            return JSONObject()
                .put("id", project.id)
                .put("projectName", project.projectName)
                .put("environment", project.environment.name)
                .put("primaryLanguage", project.primaryLanguage.name)
                .put("description", project.description)
                .put("buildInstructions", project.buildInstructions)
                .put("runInstructions", project.runInstructions)
                .put("testInstructions", project.testInstructions)
                .put("securityAuditScore", project.securityAuditScore)
                .put("createdAtFormatted", project.createdAtFormatted)
                .put("recommendedModel", model)
                .put("alternateModels", JSONArray())
                .put("files", files)
                .toString()
        }

        fun decodeProject(json: String): StudioProjectSpec {
            val o = JSONObject(json)
            val filesJson = o.optJSONArray("files") ?: JSONArray()
            val files = buildList {
                for (i in 0 until filesJson.length()) {
                    val f = filesJson.getJSONObject(i)
                    val lang = runCatching {
                        ProgrammingLanguage.valueOf(f.optString("language", ProgrammingLanguage.KOTLIN.name))
                    }.getOrDefault(ProgrammingLanguage.KOTLIN)
                    add(
                        StudioFileItem(
                            path = f.optString("path"),
                            language = lang,
                            content = f.optString("content"),
                            purpose = f.optString("purpose"),
                            isEntrypoint = f.optBoolean("isEntrypoint", false)
                        )
                    )
                }
            }
            val m = o.optJSONObject("recommendedModel") ?: JSONObject()
            val model = AiModelRecommendation(
                modelId = m.optString("modelId", "local"),
                modelName = m.optString("modelName", "Local"),
                provider = m.optString("provider", "local"),
                matchScore = m.optInt("matchScore", 0),
                strengthBadge = m.optString("strengthBadge"),
                contextWindow = m.optString("contextWindow"),
                strengths = emptyList(),
                teamRole = m.optString("teamRole"),
                rationale = m.optString("rationale"),
                isPrimaryPick = m.optBoolean("isPrimaryPick", true)
            )
            val env = runCatching {
                DevelopmentEnvironment.valueOf(o.optString("environment", DevelopmentEnvironment.MOBILE_ANDROID.name))
            }.getOrDefault(DevelopmentEnvironment.MOBILE_ANDROID)
            val lang = runCatching {
                ProgrammingLanguage.valueOf(o.optString("primaryLanguage", ProgrammingLanguage.KOTLIN.name))
            }.getOrDefault(ProgrammingLanguage.KOTLIN)
            return StudioProjectSpec(
                id = o.optString("id"),
                projectName = o.optString("projectName"),
                environment = env,
                primaryLanguage = lang,
                description = o.optString("description"),
                recommendedModel = model,
                alternateModels = emptyList(),
                files = files,
                buildInstructions = o.optString("buildInstructions"),
                runInstructions = o.optString("runInstructions"),
                testInstructions = o.optString("testInstructions"),
                securityAuditScore = if (o.isNull("securityAuditScore")) null else o.optInt("securityAuditScore"),
                createdAtFormatted = o.optString("createdAtFormatted", "")
            )
        }
    }
}
