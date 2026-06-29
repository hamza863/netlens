package com.netlens

import com.netlens.model.NetworkLogEntry
import com.netlens.redact.Redactor
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RedactionTest {

    @Before fun setUp() { Redactor.headers = Redactor.DEFAULT_HEADERS }
    @After  fun tearDown() { Redactor.headers = Redactor.DEFAULT_HEADERS }

    @Test
    fun `toCurl masks Authorization but keeps other headers`() {
        val curl = entry(
            requestHeaders = mapOf(
                "Authorization" to "Bearer secret-token",
                "Content-Type" to "application/json"
            )
        ).toCurl()

        assertFalse("token must not leak", curl.contains("secret-token"))
        assertTrue(curl.contains(Redactor.MASK))
        assertTrue("non-sensitive header preserved", curl.contains("application/json"))
    }

    @Test
    fun `redaction is case-insensitive on header name`() {
        assertEquals(Redactor.MASK, Redactor.value("authorization", "x"))
        assertEquals(Redactor.MASK, Redactor.value("AUTHORIZATION", "x"))
        assertEquals("keep", Redactor.value("X-Trace-Id", "keep"))
    }

    @Test
    fun `custom redact set replaces defaults`() {
        Redactor.headers = setOf("x-trace-id")
        assertEquals(Redactor.MASK, Redactor.value("X-Trace-Id", "abc"))
        assertEquals("Bearer t", Redactor.value("Authorization", "Bearer t"))
    }

    private fun entry(
        url: String = "https://api.example.com/test",
        requestHeaders: Map<String, String> = emptyMap()
    ) = NetworkLogEntry(
        method          = "GET",
        url             = url,
        requestHeaders  = requestHeaders,
        requestBody     = null,
        responseCode    = 200,
        responseMessage = "OK",
        responseHeaders = emptyMap(),
        responseBody    = null,
        durationMs      = 100L
    )
}
