package com.example.data.storage

import com.example.domain.model.AiModelRecommendation
import com.example.domain.model.DevelopmentEnvironment
import com.example.domain.model.ProgrammingLanguage
import com.example.domain.model.StudioFileItem
import com.example.domain.model.StudioProjectSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectStorageEngineTest {

    private fun sample(id: String = "proj_1", name: String = "Demo"): StudioProjectSpec {
        val model = AiModelRecommendation(
            modelId = "local",
            modelName = "Local",
            provider = "local",
            matchScore = 1,
            strengthBadge = "",
            contextWindow = "",
            strengths = emptyList(),
            teamRole = "",
            rationale = "",
            isPrimaryPick = true
        )
        return StudioProjectSpec(
            id = id,
            projectName = name,
            environment = DevelopmentEnvironment.MOBILE_ANDROID,
            primaryLanguage = ProgrammingLanguage.KOTLIN,
            description = "offline",
            recommendedModel = model,
            alternateModels = emptyList(),
            files = listOf(
                StudioFileItem("src/Main.kt", ProgrammingLanguage.KOTLIN, "fun main() {}", "entry", true)
            ),
            buildInstructions = "gradle",
            runInstructions = "run",
            testInstructions = "test"
        )
    }

    @Test
    fun localSaveListLoadDeleteRoundTrip() = runBlocking {
        val mem = InMemoryProjectStorageProvider()
        val project = sample()
        val saved = mem.saveProject(project, expectedRevision = null).getOrThrow()
        assertEquals(1L, saved.revision)
        assertEquals(1, mem.listProjects().getOrThrow().size)
        val loaded = mem.loadProject(project.id).getOrThrow()
        assertNotNull(loaded)
        assertEquals("Demo", loaded!!.projectName)
        mem.deleteProject(project.id).getOrThrow()
        assertTrue(mem.listProjects().getOrThrow().isEmpty())
    }

    @Test
    fun versionHistoryAndRestore() = runBlocking {
        val mem = InMemoryProjectStorageProvider()
        mem.saveProject(sample(name = "v1"), null).getOrThrow()
        mem.saveProject(sample(name = "v2"), expectedRevision = 1L).getOrThrow()
        val versions = mem.listVersions("proj_1").getOrThrow()
        assertTrue(versions.size >= 2)
        val restored = mem.restoreVersion("proj_1", 1L).getOrThrow()
        assertEquals("v1", restored.projectName)
    }

    @Test
    fun conflictKeepBothPreservesCurrentAndAddsCopy() = runBlocking {
        val mem = InMemoryProjectStorageProvider()
        mem.saveProject(sample(name = "original"), null).getOrThrow()
        val outcome = mem.saveProject(sample(name = "incoming"), expectedRevision = 0L, policy = ConflictPolicy.KEEP_BOTH)
            .getOrThrow()
        assertTrue(outcome.conflictCreated)
        assertNotNull(outcome.conflictProjectId)
        val current = mem.loadProject("proj_1").getOrThrow()
        assertEquals("original", current!!.projectName)
        val conflict = mem.loadProject(outcome.conflictProjectId!!).getOrThrow()
        assertEquals("incoming", conflict!!.projectName.replace(" (conflict)", ""))
        // name was tagged
        assertTrue(conflict.projectName.contains("conflict"))
    }

    @Test
    fun cloudProviderWithoutTokenFailsClosed() = runBlocking {
        val vault = object : com.example.core.security.SecretStorage {
            private val map = mutableMapOf<String, String>()
            override fun getSecret(key: String): String = map[key].orEmpty()
            override fun putSecret(key: String, value: String) { map[key] = value }
            override fun hasSecret(key: String): Boolean = map[key].orEmpty().isNotBlank()
            override fun clearAll() { map.clear() }
        }
        val drive = GoogleDriveStorageProvider(vault)
        val status = drive.status()
        assertFalse(status.configured)
        assertTrue(drive.listProjects().isFailure)
        assertTrue(drive.saveProject(sample(), null).isFailure)
    }

    @Test
    fun offlineDoesNotRequireBackend() = runBlocking {
        val mem = InMemoryProjectStorageProvider()
        // Saving without any network/backend
        val outcome = mem.saveProject(sample(), null).getOrThrow()
        assertEquals(StorageProviderId.LOCAL, outcome.providerId)
        assertEquals(1L, outcome.revision)
    }
}
