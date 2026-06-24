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
 *  - Binary bodies are not decoded to text (no mojibake); image bodies within the
 *    cap are kept as raw bytes so the viewer can render a preview.
 */
class NetLensInterceptor(
    private val maxBodyBytes: Long = 64 * 1024L
) : Interceptor {

    private val utf8 = Charset.forName("UTF-8")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request  = chain.request()
        val startMs  = System.currentTimeMillis()

        // ── Capture request body ──────────────────────────────────────────────
        var reqSize = 0L
        val reqBody: String? = try {
            request.body?.let { body ->
                val buffer = Buffer().also { body.writeTo(it) }
                reqSize = buffer.size
                if (isTextContent(request.header("Content-Type")))
                    buffer.readString(utf8).take(maxBodyBytes.toInt())
                else null
            }
        } catch (_: Exception) { null }

        // ── Execute call ──────────────────────────────────────────────────────
        val response: Response
        try {
            response = chain.proceed(request)
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
                    requestBodySize = reqSize,
                    error           = e.message ?: e.javaClass.simpleName
                )
            )
            throw e     // re-throw so caller still sees the error
        }

        // ── Peek at response body (never consume) ─────────────────────────────
        var respSize = 0L
        var imageBytes: ByteArray? = null
        val respBody: String? = try {
            if (response.promisesBody()) {
                val contentType = response.header("Content-Type")
                val body = response.body
                val source = body.source()
                source.request(maxBodyBytes)
                val peek = source.buffer.clone()
                val declared = body.contentLength()
                respSize = if (declared >= 0) declared else peek.size
                when {
                    isImageContent(contentType) -> {
                        if (peek.size in 1..maxBodyBytes) imageBytes = peek.readByteArray()
                        null
                    }
                    isTextContent(contentType) ->
                        peek.readString(utf8).take(maxBodyBytes.toInt())
                    else -> null   // binary — labelled in the viewer, not decoded
                }
            } else null
        } catch (_: Exception) { null }

        // Measured after peeking so it reflects time until the body is actually available.
        val durationMs = System.currentTimeMillis() - startMs

        NetworkLogStore.add(
            NetworkLogEntry(
                method            = request.method,
                url               = request.url.toString(),
                requestHeaders    = request.headers.toMap(),
                requestBody       = reqBody,
                responseCode      = response.code,
                responseMessage   = response.message,
                responseHeaders   = response.headers.toMap(),
                responseBody      = respBody,
                durationMs        = durationMs,
                requestBodySize   = reqSize,
                responseBodySize  = respSize,
                responseImageBytes = imageBytes,
                error             = null
            )
        )
        return response     // original response untouched ✅
    }

    /** Content types we can safely render as text. Unknown type → assume text (common for bare JSON). */
    private fun isTextContent(contentType: String?): Boolean {
        if (contentType.isNullOrBlank()) return true
        val c = contentType.lowercase()
        return c.contains("json") || c.contains("xml") || c.contains("text") ||
               c.contains("html") || c.contains("form-urlencoded") ||
               c.contains("javascript") || c.contains("csv")
    }

    private fun isImageContent(contentType: String?): Boolean =
        contentType?.lowercase()?.startsWith("image/") == true
}
