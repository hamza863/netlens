package com.netlens

import com.netlens.model.NetworkLogEntry
import org.junit.Assert.*
import org.junit.Test

class NetworkLogEntryTest {

    @Test fun `isSuccess true for 200`()     = assertTrue(entry(200).isSuccess)
    @Test fun `isSuccess true for 201`()     = assertTrue(entry(201).isSuccess)
    @Test fun `isSuccess true for 204`()     = assertTrue(entry(204).isSuccess)
    @Test fun `isSuccess false for 404`()    = assertFalse(entry(404).isSuccess)

    @Test fun `isRedirect true for 301`()    = assertTrue(entry(301).isRedirect)
    @Test fun `isRedirect true for 302`()    = assertTrue(entry(302).isRedirect)
    @Test fun `isRedirect false for 200`()   = assertFalse(entry(200).isRedirect)

    @Test fun `isClientError true for 400`() = assertTrue(entry(400).isClientError)
    @Test fun `isClientError true for 404`() = assertTrue(entry(404).isClientError)
    @Test fun `isClientError true for 422`() = assertTrue(entry(422).isClientError)

    @Test fun `isServerError true for 500`() = assertTrue(entry(500).isServerError)
    @Test fun `isServerError true for 503`() = assertTrue(entry(503).isServerError)

    @Test fun `isFailed true when error is set`() = assertTrue(entry(0, error = "timeout").isFailed)
    @Test fun `isFailed false when no error`()    = assertFalse(entry(200).isFailed)

    @Test fun `statusLabel returns code as string`() = assertEquals("404", entry(404).statusLabel)
    @Test fun `statusLabel returns ERR when failed`() = assertEquals("ERR", entry(0, error = "oops").statusLabel)

    @Test
    fun `shortUrl strips https scheme`() {
        val e = entry(200, url = "https://api.example.com/users")
        assertEquals("api.example.com/users", e.shortUrl)
    }

    @Test
    fun `shortUrl strips http scheme`() {
        val e = entry(200, url = "http://api.example.com/users")
        assertEquals("api.example.com/users", e.shortUrl)
    }

    @Test
    fun `formattedTime is not blank`() {
        assertFalse(entry(200).formattedTime.isBlank())
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private fun entry(
        code: Int,
        url: String  = "https://api.example.com/test",
        error: String? = null
    ) = NetworkLogEntry(
        method          = "GET",
        url             = url,
        requestHeaders  = emptyMap(),
        requestBody     = null,
        responseCode    = code,
        responseMessage = "OK",
        responseHeaders = emptyMap(),
        responseBody    = null,
        durationMs      = 100L,
        error           = error
    )
}
