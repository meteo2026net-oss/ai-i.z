package com.example.data.repository

import com.example.domain.model.ProgrammingLanguage
import com.example.domain.model.StudioFileItem
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
}
