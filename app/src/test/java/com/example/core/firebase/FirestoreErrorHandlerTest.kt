package com.example.core.firebase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreErrorHandlerTest {

    @Test
    fun diagnosticsDoNotExposeRawMessagesOrDocumentIdentifiers() {
        val rawEmail = "private.user@example.test"
        val rawUid = "user-secret-123"
        val rawDocumentId = "task-secret-456"

        val summary = handleFirestoreError(
            exception = IllegalStateException("Failure for $rawEmail and token=secret-value"),
            operationType = OperationType.GET,
            path = "users/$rawUid/tasks/$rawDocumentId"
        )

        assertTrue(summary.contains("IllegalStateException"))
        assertTrue(summary.contains("get"))
        assertTrue(summary.contains("users/{id}/tasks/{id}"))
        assertFalse(summary.contains(rawEmail))
        assertFalse(summary.contains(rawUid))
        assertFalse(summary.contains(rawDocumentId))
        assertFalse(summary.contains("secret-value"))
    }
}
