package com.netlens

import com.netlens.interceptor.NetLensInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Integration tests for [NetLensInterceptor] using OkHttp's [MockWebServer].
 * These run entirely on the JVM — no emulator, no Android device needed.
 *
 * Run with:  ./gradlew :netlens:test
 */
class NetLensInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        NetworkLogStore.clear()

        server = MockWebServer()
        server.start()

        client = OkHttpClient.Builder()
            .addInterceptor(NetLensInterceptor())
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkLogStore.clear()
    }

    // ── GET request ───────────────────────────────────────────────────────────

    @Test
    fun `GET request is logged with correct method and url`() {
        server.enqueue(MockResponse().setBody("""{"id":1}""").setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/users/1")).build()).execute()

        val log = NetworkLogStore.getAll().first()
        assertEquals("GET", log.method)
        assertTrue(log.url.endsWith("/users/1"))
    }

    @Test
    fun `GET 200 response body is captured`() {
        val body = """{"name":"Ahmad","email":"ahmad@example.com"}"""
        server.enqueue(MockResponse().setBody(body).setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/profile")).build()).execute()

        val log = NetworkLogStore.getAll().first()
        assertEquals(200, log.responseCode)
        assertEquals(body, log.responseBody)
    }

    @Test
    fun `GET response is still readable by caller after interception`() {
        val expected = """{"result":"intact"}"""
        server.enqueue(MockResponse().setBody(expected).setResponseCode(200))

        val response = client.newCall(
            Request.Builder().url(server.url("/check")).build()
        ).execute()

        // Caller must be able to read the full body — peek must not have consumed it
        assertEquals(expected, response.body?.string())
    }

    // ── POST request ──────────────────────────────────────────────────────────

    @Test
    fun `POST request body is captured`() {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))

        val requestJson = """{"username":"ahmad","password":"secret123"}"""
        client.newCall(
            Request.Builder()
                .url(server.url("/login"))
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .build()
        ).execute()

        val log = NetworkLogStore.getAll().first()
        assertEquals("POST", log.method)
        assertEquals(requestJson, log.requestBody)
        assertEquals(201, log.responseCode)
    }

    // ── Error responses ───────────────────────────────────────────────────────

    @Test
    fun `404 response is logged with correct status`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"not found"}"""))

        client.newCall(Request.Builder().url(server.url("/missing")).build()).execute()

        val log = NetworkLogStore.getAll().first()
        assertEquals(404, log.responseCode)
        assertTrue(log.isClientError)
        assertFalse(log.isSuccess)
    }

    @Test
    fun `500 response is logged correctly`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"server error"}"""))

        client.newCall(Request.Builder().url(server.url("/crash")).build()).execute()

        val log = NetworkLogStore.getAll().first()
        assertEquals(500, log.responseCode)
        assertTrue(log.isServerError)
    }

    // ── Network failure ───────────────────────────────────────────────────────

    @Test
    fun `network failure logs an error entry`() {
        // Queue a connection close to simulate network failure
        server.enqueue(MockResponse().setSocketPolicy(
            okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST
        ))

        try {
            client.newCall(Request.Builder().url(server.url("/fail")).build()).execute()
        } catch (_: Exception) { /* expected */ }

        // An error entry should still be logged
        val log = NetworkLogStore.getAll().firstOrNull()
        assertNotNull("Expected an error log entry", log)
        assertTrue("Expected failed entry", log!!.isFailed || log.responseCode == 0)
    }

    // ── Headers ───────────────────────────────────────────────────────────────

    @Test
    fun `request headers are captured`() {
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(
            Request.Builder()
                .url(server.url("/secure"))
                .addHeader("Authorization", "Bearer test-token")
                .addHeader("X-App-Version", "1.0.0")
                .build()
        ).execute()

        val log = NetworkLogStore.getAll().first()
        assertEquals("Bearer test-token", log.requestHeaders["Authorization"])
        assertEquals("1.0.0", log.requestHeaders["X-App-Version"])
    }

    @Test
    fun `response headers are captured`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("X-Request-Id", "abc-123")
                .addHeader("Cache-Control", "no-cache")
        )

        client.newCall(Request.Builder().url(server.url("/headers")).build()).execute()

        val log = NetworkLogStore.getAll().first()
        assertEquals("abc-123", log.responseHeaders["X-Request-Id"])
    }

    // ── Duration ──────────────────────────────────────────────────────────────

    @Test
    fun `duration is captured and greater than zero`() {
        server.enqueue(MockResponse().setBodyDelay(50, TimeUnit.MILLISECONDS).setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/slow")).build()).execute()

        val log = NetworkLogStore.getAll().first()
        assertTrue("Duration should be > 0ms", log.durationMs > 0)
        assertTrue("Duration should be >= 50ms (server delay)", log.durationMs >= 50)
    }

    // ── Multiple requests ─────────────────────────────────────────────────────

    @Test
    fun `multiple requests are all logged in newest-first order`() {
        repeat(3) { i ->
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{"i":$i}"""))
        }

        client.newCall(Request.Builder().url(server.url("/a")).build()).execute()
        client.newCall(Request.Builder().url(server.url("/b")).build()).execute()
        client.newCall(Request.Builder().url(server.url("/c")).build()).execute()

        val logs = NetworkLogStore.getAll()
        assertEquals(3, logs.size)
        assertTrue("Newest request should be first", logs.first().url.endsWith("/c"))
        assertTrue("Oldest request should be last",  logs.last().url.endsWith("/a"))
    }

    // ── Body cap ──────────────────────────────────────────────────────────────

    @Test
    fun `body larger than maxBodyBytes is capped`() {
        val bigBody = "x".repeat(200 * 1024) // 200 KB
        server.enqueue(MockResponse().setResponseCode(200).setBody(bigBody))

        val cappedClient = OkHttpClient.Builder()
            .addInterceptor(NetLensInterceptor(maxBodyBytes = 1024L))  // 1 KB cap
            .build()

        cappedClient.newCall(Request.Builder().url(server.url("/big")).build()).execute()

        val log = NetworkLogStore.getAll().first()
        assertTrue(
            "Captured body should be ≤ 1024 chars",
            (log.responseBody?.length ?: 0) <= 1024
        )
    }

    // ── NetworkLogEntry helpers ───────────────────────────────────────────────

    @Test
    fun `isSuccess is true for 2xx codes`() {
        for (code in listOf(200, 201, 204)) {
            server.enqueue(MockResponse().setResponseCode(code))
            client.newCall(Request.Builder().url(server.url("/x")).build()).execute()
        }
        val logs = NetworkLogStore.getAll()
        assertTrue(logs.all { it.isSuccess })
    }
}
