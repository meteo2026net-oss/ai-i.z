package com.example.data.repository

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Date

class ServerDateJsonAdapterTest {
    private val adapter = Moshi.Builder()
        .add(Date::class.java, ServerDateJsonAdapter)
        .build()
        .adapter(Date::class.java)

    @Test
    fun parsesUtcTimestampWithMilliseconds() {
        val parsed = adapter.fromJson("\"2026-11-16T00:00:00.123Z\"")
        assertEquals(1794787200123L, parsed?.time)
    }

    @Test
    fun parsesTimestampWithExplicitOffset() {
        val parsed = adapter.fromJson("\"2026-11-16T03:00:00+03\"")
        assertEquals(1794787200000L, parsed?.time)
    }

    @Test
    fun rejectsImpossibleCalendarDateInsteadOfNormalizingIt() {
        assertThrows(IllegalArgumentException::class.java) {
            adapter.fromJson("\"2026-02-30T12:00:00Z\"")
        }
    }

    @Test
    fun rejectsTrailingCharacters() {
        assertThrows(IllegalArgumentException::class.java) {
            adapter.fromJson("\"2026-11-16T12:00:00Zgarbage\"")
        }
    }

    @Test
    fun retainsEpochMillisecondCompatibility() {
        assertEquals(1794787200000L, adapter.fromJson("\"1794787200000\"")?.time)
    }

    @Test
    fun writesUtcTimestamp() {
        assertEquals("\"1970-01-01T00:00:00.000Z\"", adapter.toJson(Date(0)))
    }

    @Test
    fun readsNull() {
        assertNull(adapter.fromJson("null"))
    }
}
