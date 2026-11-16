package com.example.data.repository

import com.example.domain.model.DevelopmentEnvironment
import com.example.domain.model.ProgrammingLanguage
import com.example.domain.model.StudioFileItem
import com.example.domain.model.StudioProjectSummary
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * End-to-end unit regression for Universal Studio:
 * create → list summary → open race → edit guard → export → conflict recovery.
 */
class StudioProjectLifecycleTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test
    fun generateThenSummarizeThenExportRoundTrip() {
        val project = UniversalStudioProjectGenerator.generateProject(
            name = "lifecycle_demo",
            prompt = "Simple CRUD API with auth",
            environment = DevelopmentEnvironment.BACKEND_NODE,
            language = ProgrammingLanguage.TYPESCRIPT
        )
        assertTrue(project.files.isNotEmpty())
        assertTrue(project.id.isNotBlank())
        assertTrue(project.projectName.isNotBlank())

        val summary = moshi.adapter(StudioProjectSummary::class.java).fromJson(
            """
            {
              "id": "${project.id}",
              "projectName": ${moshi.adapter(String::class.java).toJson(project.projectName)},
              "environment": "${project.environment.name}",
              "primaryLanguage": "${project.primaryLanguage.name}",
              "description": "lifecycle",
              "fileCount": ${project.files.size}
            }
            """.trimIndent()
        )
        assertNotNull(summary)
        assertEquals(project.id, summary!!.id)
        assertEquals(project.files.size, summary.fileCount)

        val out = ByteArrayOutputStream()
        StudioProjectArchiveExporter.writeZip(project, out)
        val zipBytes = out.toByteArray()
        assertTrue(zipBytes.size > 32)
        // ZIP local file header magic
        assertEquals(0x50, zipBytes[0].toInt() and 0xff)
        assertEquals(0x4b, zipBytes[1].toInt() and 0xff)
    }

    @Test
    fun networkRaceOpenAndSaveOnlyLatestApplies() {
        val openTracker = StudioProjectOpenRequestTracker()
        val saveTracker = StudioProjectSaveRevisionTracker()

        val openA = openTracker.next()
        val openB = openTracker.next()
        assertFalse(openTracker.isCurrent(openA))
        assertTrue(openTracker.isCurrent(openB))

        // Simulate failed open recovery: invalidate then refresh library.
        openTracker.invalidate()
        assertFalse(openTracker.isCurrent(openB))
        val recovery = openTracker.next()
        assertTrue(openTracker.isCurrent(recovery))

        val save1 = saveTracker.next("lifecycle_demo")
        val save2 = saveTracker.next("lifecycle_demo")
        assertFalse(saveTracker.isCurrent("lifecycle_demo", save1))
        assertTrue(saveTracker.isCurrent("lifecycle_demo", save2))
    }

    @Test
    fun editorRejectsStaleSelectionAndAppliesValidEdit() {
        val files = listOf(
            StudioFileItem(path = "src/index.ts", language = ProgrammingLanguage.TYPESCRIPT, content = "export {}", purpose = "entry", isEntrypoint = true),
            StudioFileItem(path = "README.md", language = ProgrammingLanguage.TYPESCRIPT, content = "docs", purpose = "docs", isEntrypoint = false)
        )
        assertNull(StudioProjectFileEditGuard.updateSelectedFileContent(files, "missing.ts", "x"))
        assertNull(StudioProjectFileEditGuard.updateSelectedFileContent(files, "", "x"))
        val updated = StudioProjectFileEditGuard.updateSelectedFileContent(files, "src/index.ts", "export const ok = 1")
        assertNotNull(updated)
        assertEquals("export const ok = 1", updated!!.first { it.path == "src/index.ts" }.content)
        assertEquals("docs", updated.first { it.path == "README.md" }.content)
    }

    @Test
    fun exportRejectsTraversalPathsAndParserRejectsSecrets() {
        val bad = UniversalStudioProjectGenerator.generateProject(
            name = "safe",
            prompt = "x",
            environment = DevelopmentEnvironment.BACKEND_NODE,
            language = ProgrammingLanguage.TYPESCRIPT
        ).copy(
            files = listOf(
                StudioFileItem(path = "../outside.ts", language = ProgrammingLanguage.TYPESCRIPT, content = "x", purpose = "bad", isEntrypoint = true)
            )
        )
        assertThrows(IllegalArgumentException::class.java) {
            StudioProjectArchiveExporter.writeZip(bad, ByteArrayOutputStream())
        }

        assertThrows(IllegalArgumentException::class.java) {
            StudioAiProjectResponseParser.parseFiles(
                """{"files":[{"path":".env","content":"SECRET=1","isEntrypoint":true}]}""",
                ProgrammingLanguage.TYPESCRIPT
            )
        }
    }
}
