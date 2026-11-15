package com.example.data.repository

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks the newest requested cloud-save revision for each Studio project.
 * A stale asynchronous save must not overwrite a newer edit or update its UI state.
 */
class StudioProjectSaveRevisionTracker {
    private val revisions = ConcurrentHashMap<String, AtomicLong>()

    fun next(projectId: String): Long {
        require(projectId.isNotBlank()) { "Project ID must not be blank." }
        return revisions.computeIfAbsent(projectId) { AtomicLong(0L) }.incrementAndGet()
    }

    fun isCurrent(projectId: String, revision: Long): Boolean =
        revisions[projectId]?.get() == revision
}
