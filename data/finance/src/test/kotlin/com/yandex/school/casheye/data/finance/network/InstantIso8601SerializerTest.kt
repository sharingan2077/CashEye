package com.yandex.school.casheye.data.finance.network

import com.yandex.school.casheye.data.finance.mapper.InstantIso8601Serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class InstantIso8601SerializerTest {
    @Test
    fun `serializes and deserializes ISO-8601 instants`() {
        val instant = Instant.parse("2026-07-17T12:34:56Z")

        val encoded = Json.encodeToString(InstantIso8601Serializer, instant)

        assertEquals("\"2026-07-17T12:34:56Z\"", encoded)
        assertEquals(instant, Json.decodeFromString(InstantIso8601Serializer, encoded))
    }
}
