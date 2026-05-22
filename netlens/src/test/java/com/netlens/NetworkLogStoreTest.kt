package com.netlens

import com.netlens.model.NetworkLogEntry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NetworkLogStoreTest {

    @Before
    fun setUp() {
        NetworkLogStore.clear()
        NetworkLogStore.maxEntries = 200
    }

    @After
    fun tearDown() = NetworkLogStore.clear()

    // ── add / getAll ──────────────────────────────────────────────────────────

    @Test
    fun `add stores entry and getAll returns it`() {
        NetworkLogStore.add(fakeEntry(url = "https://api.example.com/users"))
        assertEquals(1, NetworkLogStore.size())
        assertEquals("https://api.example.com/users", NetworkLogStore.getAll().first().url)
    }

    @Test
    fun `newest entry is always first`() {
        NetworkLogStore.add(fakeEntry(url = "https://api.example.com/first"))
        Thread.sleep(5)
        NetworkLogStore.add(fakeEntry(url = "https://api.example.com/second"))

        assertEquals("https://api.example.com/second", NetworkLogStore.getAll().first().url)
    }

    // ── circular buffer ───────────────────────────────────────────────────────

    @Test
    fun `circular buffer drops oldest entries when full`() {
        NetworkLogStore.maxEntries = 5
        repeat(7) { i -> NetworkLogStore.add(fakeEntry(url = "https://api.example.com/$i")) }

        assertEquals(5, NetworkLogStore.size())
        // Newest (6, 5, 4, 3, 2) should be present; 0 and 1 should be dropped
        val urls = NetworkLogStore.getAll().map { it.url }
        assertFalse(urls.any { it.endsWith("/0") })
        assertFalse(urls.any { it.endsWith("/1") })
        assertTrue(urls.any { it.endsWith("/6") })
    }

    @Test
    fun `maxEntries = 1 keeps only the latest call`() {
        NetworkLogStore.maxEntries = 1
        NetworkLogStore.add(fakeEntry(url = "https://api.example.com/old"))
        NetworkLogStore.add(fakeEntry(url = "https://api.example.com/new"))

        assertEquals(1, NetworkLogStore.size())
        assertEquals("https://api.example.com/new", NetworkLogStore.getAll().first().url)
    }

    // ── filter ────────────────────────────────────────────────────────────────

    @Test
    fun `filter by URL substring works`() {
        NetworkLogStore.add(fakeEntry(url = "https://api.example.com/users"))
        NetworkLogStore.add(fakeEntry(url = "https://api.example.com/products"))

        val results = NetworkLogStore.filter("users")
        assertEquals(1, results.size)
        assertTrue(results.first().url.contains("users"))
    }

    @Test
    fun `filter by method works`() {
        NetworkLogStore.add(fakeEntry(method = "GET"))
        NetworkLogStore.add(fakeEntry(method = "POST"))
        NetworkLogStore.add(fakeEntry(method = "DELETE"))

        val results = NetworkLogStore.filter("post")
        assertEquals(1, results.size)
        assertEquals("POST", results.first().method)
    }

    @Test
    fun `filter by status code prefix works`() {
        NetworkLogStore.add(fakeEntry(responseCode = 200))
        NetworkLogStore.add(fakeEntry(responseCode = 404))
        NetworkLogStore.add(fakeEntry(responseCode = 500))

        val results = NetworkLogStore.filter("4")
        assertEquals(1, results.size)
        assertEquals(404, results.first().responseCode)
    }

    @Test
    fun `empty filter query returns all entries`() {
        repeat(3) { NetworkLogStore.add(fakeEntry()) }
        assertEquals(3, NetworkLogStore.filter("").size)
    }

    // ── clear ─────────────────────────────────────────────────────────────────

    @Test
    fun `clear removes all entries`() {
        repeat(5) { NetworkLogStore.add(fakeEntry()) }
        NetworkLogStore.clear()
        assertEquals(0, NetworkLogStore.size())
        assertTrue(NetworkLogStore.getAll().isEmpty())
    }

    // ── thread safety ─────────────────────────────────────────────────────────

    @Test
    fun `concurrent writes do not throw`() {
        val threads = (1..20).map { i ->
            Thread { NetworkLogStore.add(fakeEntry(url = "https://example.com/$i")) }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        // Should not throw; exact count depends on timing but must be ≤ maxEntries
        assertTrue(NetworkLogStore.size() <= NetworkLogStore.maxEntries)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeEntry(
        url: String          = "https://api.example.com/test",
        method: String       = "GET",
        responseCode: Int    = 200,
        durationMs: Long     = 123L
    ) = NetworkLogEntry(
        method          = method,
        url             = url,
        requestHeaders  = mapOf("Content-Type" to "application/json"),
        requestBody     = null,
        responseCode    = responseCode,
        responseMessage = "OK",
        responseHeaders = mapOf("Content-Type" to "application/json"),
        responseBody    = """{"status":"ok"}""",
        durationMs      = durationMs
    )
}
