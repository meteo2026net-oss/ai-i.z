package com.example.data.repository

import java.util.concurrent.atomic.AtomicLong

/**
 * Prevents stale asynchronous project-open responses from replacing newer workspace state.
 */
class StudioProjectOpenRequestTracker {
    private val generation = AtomicLong(0L)

    fun next(): Long = generation.incrementAndGet()

    fun invalidate(): Long = generation.incrementAndGet()

    fun isCurrent(requestId: Long): Boolean = generation.get() == requestId
}
