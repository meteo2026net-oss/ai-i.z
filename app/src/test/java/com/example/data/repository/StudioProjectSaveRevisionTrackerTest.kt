package com.example.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioProjectSaveRevisionTrackerTest {

    @Test
    fun newerSaveInvalidatesAnOlderQueuedSave() {
        val tracker = StudioProjectSaveRevisionTracker()

        val first = tracker.next("project-1")
        val second = tracker.next("project-1")

        assertTrue(second > first)
        assertFalse(tracker.isCurrent("project-1", first))
        assertTrue(tracker.isCurrent("project-1", second))
    }

    @Test
    fun revisionsAreIndependentPerProject() {
        val tracker = StudioProjectSaveRevisionTracker()

        val firstProjectRevision = tracker.next("project-1")
        val secondProjectRevision = tracker.next("project-2")
        tracker.next("project-1")

        assertFalse(tracker.isCurrent("project-1", firstProjectRevision))
        assertTrue(tracker.isCurrent("project-2", secondProjectRevision))
    }

    @Test
    fun unknownProjectsNeverHaveAValidRevision() {
        val tracker = StudioProjectSaveRevisionTracker()

        assertFalse(tracker.isCurrent("unknown", 1L))
    }
}
