package com.netlens.model

import java.text.SimpleDateFormat
import java.util.*

data class NetworkLogEntry(
    val id: Long = System.currentTimeMillis(),
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val responseCode: Int,
    val responseMessage: String,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val durationMs: Long,
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
}
