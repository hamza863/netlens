package com.netlens.model

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong

data class NetworkLogEntry(
    val id: Long = nextId(),
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val responseCode: Int,
    val responseMessage: String,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val durationMs: Long,
    val requestBodySize: Long = 0L,
    val responseBodySize: Long = 0L,
    /** Raw bytes of an image response, captured only when the body is an image within the cap. */
    val responseImageBytes: ByteArray? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val error: String? = null         // non-null if the call threw an exception
) {
    val isSuccess: Boolean     get() = responseCode in 200..299
    val isRedirect: Boolean    get() = responseCode in 300..399
    val isClientError: Boolean get() = responseCode in 400..499
    val isServerError: Boolean get() = responseCode >= 500
    val isFailed: Boolean      get() = error != null

    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    val shortUrl: String
        get() = url.removePrefix("https://").removePrefix("http://")

    val statusLabel: String
        get() = if (isFailed) "ERR" else responseCode.toString()

    val requestContentType: String?  get() = header(requestHeaders, "Content-Type")
    val responseContentType: String? get() = header(responseHeaders, "Content-Type")
    val isImageResponse: Boolean     get() = responseContentType?.startsWith("image/", true) == true

    val formattedReqSize: String  get() = formatBytes(requestBodySize)
    val formattedRespSize: String get() = formatBytes(responseBodySize)

    /**
     * Reconstruct this call as a runnable `curl` command. Sensitive header values
     * are masked (see [com.netlens.redact.Redactor]) so the output is safe to share.
     */
    fun toCurl(): String = buildString {
        append("curl -X ").append(method)
        requestHeaders.forEach { (k, v) ->
            val value = com.netlens.redact.Redactor.value(k, v)
            append(" \\\n  -H '").append(k).append(": ").append(escapeQuote(value)).append("'")
        }
        if (!requestBody.isNullOrBlank()) {
            append(" \\\n  -d '").append(escapeQuote(requestBody)).append("'")
        }
        append(" \\\n  '").append(url).append("'")
    }

    private fun escapeQuote(s: String) = s.replace("'", "'\\''")

    // ByteArray fields need content-aware equals/hashCode for a correct data class.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NetworkLogEntry) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        private val seq = AtomicLong(0L)
        private fun nextId(): Long = seq.incrementAndGet()

        private fun header(h: Map<String, String>, name: String): String? =
            h.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

        fun formatBytes(bytes: Long): String = when {
            bytes <= 0        -> "—"
            bytes < 1024      -> "$bytes B"
            bytes < 1_048_576 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else              -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
        }
    }
}
