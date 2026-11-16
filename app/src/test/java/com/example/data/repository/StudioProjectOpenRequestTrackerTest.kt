package com.example.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioProjectOpenRequestTrackerTest {
    @Test
    fun newerOpenRequestInvalidatesOlderResponse() {
        val tracker = StudioProjectOpenRequestTracker()
        val first = tracker.next()
        val second = tracker.next()

        assertFalse(tracker.isCurrent(first))
        assertTrue(tracker.isCurrent(second))
    }

    @Test
    fun deletionInvalidatesAnInFlightOpenRequest() {
        val tracker = StudioProjectOpenRequestTracker()
        val pending = tracker.next()

        tracker.invalidate()

        assertFalse(tracker.isCurrent(pending))
    }

    @Test
    fun deletionInvalidatesAnInFlightLibraryRefresh() {
        val tracker = StudioProjectOpenRequestTracker()
        val pendingRefresh = tracker.next()

        tracker.invalidate()

        assertFalse(tracker.isCurrent(pendingRefresh))
    }

    @Test
    fun workspaceEditsInvalidatePendingSavedProjectOpenResponses() {
        val tracker = StudioProjectOpenRequestTracker()
        val pendingOpen = tracker.next()

        // Selecting a file, editing content, saving, or creating another project
        // represents newer workspace intent and must invalidate the pending open.
        tracker.invalidate()

        assertFalse(tracker.isCurrent(pendingOpen))
    }

    @Test
    fun openingSavedProjectInvalidatesAnInFlightGeneratedProject() {
        val tracker = StudioProjectOpenRequestTracker()
        val generationRequest = tracker.invalidate()
        val openRequest = tracker.next()

        assertFalse(tracker.isCurrent(generationRequest))
        assertTrue(tracker.isCurrent(openRequest))
    }

    @Test
    fun newerProjectGenerationInvalidatesAnOlderSavedProjectOpen() {
        val tracker = StudioProjectOpenRequestTracker()
        val openRequest = tracker.next()
        val generationRequest = tracker.invalidate()

        assertFalse(tracker.isCurrent(openRequest))
        assertTrue(tracker.isCurrent(generationRequest))
    }

    @Test
    fun failedDeletionCanBeFollowedByANewOpenRequest() {
        val tracker = StudioProjectOpenRequestTracker()
        val pending = tracker.next()
        tracker.invalidate()
        val retry = tracker.next()

        assertFalse(tracker.isCurrent(pending))
        assertTrue(tracker.isCurrent(retry))
    }
}
