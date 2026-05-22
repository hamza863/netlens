package com.netlens.interceptor

import com.netlens.NetworkLogStore
import com.netlens.model.NetworkLogEntry
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import java.nio.charset.Charset

/**
 * OkHttp [Interceptor] that captures request/response pairs with zero impact
 * on your API latency:
 *
 *  - Response body is **peeked** (not consumed) via [Buffer.clone] — the original
 *    stream is never touched, so Retrofit/Gson/Moshi parse as normal.
 *  - Logging is synchronous but trivially fast (memory write only, no I/O).
 *  - Body capture is capped at [maxBodyBytes] so huge downloads don't fill RAM.
 */
class NetLensInterceptor(
    private val maxBodyBytes: Long = 64 * 1024L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request  = chain.request()
        val startMs  = System.currentTimeMillis()

        // ── Capture request body ──────────────────────────────────────────────
        val reqBody: String? = try {
            request.body?.let { body ->
                Buffer().also { body.writeTo(it) }
                    .readString(Charset.forName("UTF-8"))
                    .take(maxBodyBytes.toInt())
            }
        } catch (_: Exception) { null }

        // ── Execute call ──────────────────────────────────────────────────────
        val response: Response
        val error: String?
        try {
            response = chain.proceed(request)
            error    = null
        } catch (e: Exception) {
            NetworkLogStore.add(
                NetworkLogEntry(
                    method          = request.method,
                    url             = request.url.toString(),
                    requestHeaders  = request.headers.toMap(),
                    requestBody     = reqBody,
                    responseCode    = 0,
                    responseMessage = "",
                    responseHeaders = emptyMap(),
                    responseBody    = null,
                    durationMs      = System.currentTimeMillis() - startMs,
                    error           = e.message ?: e.javaClass.simpleName
                )
            )
            throw e     // re-throw so caller still sees the error
        }

        val durationMs = System.currentTimeMillis() - startMs

        // ── Peek at response body (never consume) ─────────────────────────────
        val respBody: String? = try {
            if (response.promisesBody()) {
                response.body?.source()?.also { it.request(maxBodyBytes) }
                    ?.buffer?.clone()
                    ?.readString(Charset.forName("UTF-8"))
                    ?.take(maxBodyBytes.toInt())
            } else null
        } catch (_: Exception) { null }

        NetworkLogStore.add(
            NetworkLogEntry(
                method          = request.method,
                url             = request.url.toString(),
                requestHeaders  = request.headers.toMap(),
                requestBody     = reqBody,
                responseCode    = response.code,
                responseMessage = response.message,
                responseHeaders = response.headers.toMap(),
                responseBody    = respBody,
                durationMs      = durationMs,
                error           = error
            )
        )
        return response     // original response untouched ✅
    }
}
