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
    fun failedDeletionCanBeFollowedByANewOpenRequest() {
        val tracker = StudioProjectOpenRequestTracker()
        val pending = tracker.next()
        tracker.invalidate()
        val retry = tracker.next()

        assertFalse(tracker.isCurrent(pending))
        assertTrue(tracker.isCurrent(retry))
    }
}
