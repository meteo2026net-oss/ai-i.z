package com.example.data.repository

import com.example.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class StudioProjectArchiveExporterTest {

    @Test
    fun exportsProjectFilesWithTheirRelativePaths() {
        val output = ByteArrayOutputStream()
        StudioProjectArchiveExporter.writeZip(project(), output)

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }

        assertEquals("fun main() = Unit", entries["src/Main.kt"])
        assertEquals("# Demo", entries["README.md"])
    }

    @Test
    fun rejectsPathTraversalBeforeWritingArchive() {
        val unsafe = project().copy(
            files = listOf(project().files.first().copy(path = "../outside.txt"))
        )
        assertThrows(IllegalArgumentException::class.java) {
            StudioProjectArchiveExporter.writeZip(unsafe, ByteArrayOutputStream())
        }
    }

    @Test
    fun rejectsDuplicatePathsIgnoringCase() {
        val base = project()
        val duplicate = base.copy(files = listOf(
            base.files.first(),
            base.files.first().copy(path = "src/main.kt", isEntrypoint = false)
        ))
        assertThrows(IllegalArgumentException::class.java) {
            StudioProjectArchiveExporter.writeZip(duplicate, ByteArrayOutputStream())
        }
    }

    @Test
    fun rejectsEmptyProject() {
        assertThrows(IllegalArgumentException::class.java) {
            StudioProjectArchiveExporter.writeZip(
                project().copy(files = emptyList()),
                ByteArrayOutputStream()
            )
        }
    }

    @Test
    fun rejectsMoreThanTwentyFourFiles() {
        val template = project().files.first()
        val tooMany = (1..25).map { index ->
            template.copy(path = "src/File$index.kt", isEntrypoint = index == 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            StudioProjectArchiveExporter.writeZip(
                project().copy(files = tooMany),
                ByteArrayOutputStream()
            )
        }
    }

    @Test
    fun rejectsSingleFileLargerThanPerFileLimit() {
        val oversized = project().copy(files = listOf(
            project().files.first().copy(content = "x".repeat(256_001))
        ))
        assertThrows(IllegalArgumentException::class.java) {
            StudioProjectArchiveExporter.writeZip(oversized, ByteArrayOutputStream())
        }
    }

    @Test
    fun rejectsProjectLargerThanTotalByteLimit() {
        val template = project().files.first()
        val largeFiles = (1..8).map { index ->
            template.copy(
                path = "src/File$index.kt",
                content = "x".repeat(250_001),
                isEntrypoint = index == 1
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            StudioProjectArchiveExporter.writeZip(
                project().copy(files = largeFiles),
                ByteArrayOutputStream()
            )
        }
    }

    @Test
    fun rejectsWindowsReservedAndNonPortableSegments() {
        val template = project().files.first()
        listOf("CON.txt", "src/NUL", "trailing./file.kt", "folder/name ").forEach { unsafePath ->
            assertThrows(IllegalArgumentException::class.java) {
                StudioProjectArchiveExporter.writeZip(
                    project().copy(files = listOf(template.copy(path = unsafePath))),
                    ByteArrayOutputStream()
                )
            }
        }
    }

    @Test
    fun rejectsUnicodeCanonicalDuplicatePaths() {
        val template = project().files.first()
        val composed = "caf\u00e9.txt"
        val decomposed = "cafe\u0301.txt"
        val duplicate = project().copy(files = listOf(
            template.copy(path = composed),
            template.copy(path = decomposed, isEntrypoint = false)
        ))
        assertThrows(IllegalArgumentException::class.java) {
            StudioProjectArchiveExporter.writeZip(duplicate, ByteArrayOutputStream())
        }
    }

    @Test
    fun rejectsAbsoluteAndGitInternalPaths() {
        val template = project().files.first()
        listOf("/etc/passwd", ".git/config").forEach { unsafePath ->
            assertThrows(IllegalArgumentException::class.java) {
                StudioProjectArchiveExporter.writeZip(
                    project().copy(files = listOf(template.copy(path = unsafePath))),
                    ByteArrayOutputStream()
                )
            }
        }
    }


    @Test
    fun validatesAllEntriesBeforeWritingToOutputStream() {
        val base = project()
        val invalidLastEntry = base.copy(files = listOf(
            base.files.first(),
            base.files.last().copy(path = "../outside.txt")
        ))
        val output = ByteArrayOutputStream()

        assertThrows(IllegalArgumentException::class.java) {
            StudioProjectArchiveExporter.writeZip(invalidLastEntry, output)
        }
        assertEquals(0, output.size())
    }

    private fun project(): StudioProjectSpec {
        val model = AiModelRecommendation(
            modelId = "test",
            modelName = "Test",
            provider = "Test",
            matchScore = 90,
            strengthBadge = "Test",
            contextWindow = "1k",
            strengths = emptyList(),
            teamRole = "Test",
            rationale = "Test",
            isPrimaryPick = true
        )
        return StudioProjectSpec(
            id = "test-project",
            projectName = "demo",
            environment = DevelopmentEnvironment.MOBILE_ANDROID,
            primaryLanguage = ProgrammingLanguage.KOTLIN,
            description = "Test project",
            recommendedModel = model,
            alternateModels = emptyList(),
            files = listOf(
                StudioFileItem(
                    path = "src/Main.kt",
                    language = ProgrammingLanguage.KOTLIN,
                    content = "fun main() = Unit",
                    purpose = "Entry",
                    isEntrypoint = true
                ),
                StudioFileItem(
                    path = "README.md",
                    language = ProgrammingLanguage.KOTLIN,
                    content = "# Demo",
                    purpose = "Guide"
                )
            ),
            buildInstructions = "gradle build",
            runInstructions = "gradle run",
            testInstructions = "gradle test"
        )
    }
}
