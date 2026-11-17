package com.example.data.repository

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Strict parser for API timestamps. Rejects normalized impossible dates and
 * trailing garbage instead of silently converting them into a different instant.
 */
internal object ServerDateJsonAdapter : JsonAdapter<Date>() {
    private val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX"
    )

    override fun fromJson(reader: JsonReader): Date? {
        if (reader.peek() == JsonReader.Token.NULL) return reader.nextNull()
        val raw = reader.nextString()

        for (format in formats) {
            val parser = SimpleDateFormat(format, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val position = ParsePosition(0)
            val parsed = parser.parse(raw, position)
            if (parsed != null && position.errorIndex < 0 && position.index == raw.length) {
                return parsed
            }
        }

        return raw.toLongOrNull()?.let(::Date)
            ?: throw IllegalArgumentException("Invalid server timestamp.")
    }

    override fun toJson(writer: JsonWriter, value: Date?) {
        if (value == null) {
            writer.nullValue()
        } else {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            writer.value(formatter.format(value))
        }
    }
}
