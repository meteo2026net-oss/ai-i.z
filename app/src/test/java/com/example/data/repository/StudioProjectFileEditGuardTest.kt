package com.example.data.repository

import com.example.domain.model.ProgrammingLanguage
import com.example.domain.model.StudioFileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioProjectFileEditGuardTest {

    private val files = listOf(
        StudioFileItem(
            path = "src/Main.kt",
            language = ProgrammingLanguage.KOTLIN,
            content = "fun main() = Unit",
            purpose = "Application entry point",
            isEntrypoint = true
        )
    )

    @Test
    fun existingSelectedFileCanBeSaved() {
        assertTrue(StudioProjectFileEditGuard.hasValidSelection(files, "src/Main.kt"))
    }

    @Test
    fun staleSelectedFileIsRejected() {
        assertFalse(StudioProjectFileEditGuard.hasValidSelection(files, "src/Removed.kt"))
    }

    @Test
    fun blankSelectedPathIsRejected() {
        assertFalse(StudioProjectFileEditGuard.hasValidSelection(files, "  "))
    }

    @Test
    fun emptyProjectCannotAcceptASelection() {
        assertFalse(StudioProjectFileEditGuard.hasValidSelection(emptyList(), "src/Main.kt"))
    }

    @Test
    fun updatingSelectedFilePreservesOtherFilesAndMetadata() {
        val secondFile = StudioFileItem(
            path = "README.md",
            language = ProgrammingLanguage.KOTLIN,
            content = "original readme",
            purpose = "Project documentation",
            isEntrypoint = false
        )
        val original = files + secondFile

        val updated = StudioProjectFileEditGuard.updateSelectedFileContent(
            files = original,
            selectedPath = "src/Main.kt",
            newContent = "fun main() = println(\"updated\")"
        )

        assertTrue(updated != null)
        assertEquals("fun main() = println(\"updated\")", updated!![0].content)
        assertEquals("README.md", updated[1].path)
        assertEquals("original readme", updated[1].content)
        assertFalse(updated[1].isEntrypoint)
    }

    @Test
    fun updatingWithStaleSelectionReturnsNull() {
        assertEquals(
            null,
            StudioProjectFileEditGuard.updateSelectedFileContent(
                files = files,
                selectedPath = "src/Removed.kt",
                newContent = "must not be applied"
            )
        )
    }
}
