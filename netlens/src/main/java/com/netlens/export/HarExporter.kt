package com.netlens.export

import com.netlens.model.NetworkLogEntry
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Serialises captured entries to the [HAR 1.2](http://www.softwareishard.com/blog/har-12-spec/)
 * format — the same JSON Chrome DevTools, Charles and Proxyman import. Lets users
 * hand off a full session as a standard, tool-agnostic file.
 */
internal object HarExporter {

    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    fun toHar(entries: List<NetworkLogEntry>): String {
        val har = JSONObject().apply {
            put("log", JSONObject().apply {
                put("version", "1.2")
                put("creator", JSONObject().put("name", "NetLens").put("version", "1.0"))
                // HAR is chronological; the store keeps newest-first.
                put("entries", JSONArray().also { arr ->
                    entries.asReversed().forEach { arr.put(entry(it)) }
                })
            })
        }
        return har.toString(2)
    }

    private fun entry(e: NetworkLogEntry): JSONObject = JSONObject().apply {
        put("startedDateTime", iso.format(Date(e.timestamp)))
        put("time", e.durationMs)
        put("request", JSONObject().apply {
            put("method", e.method)
            put("url", e.url)
            put("httpVersion", "HTTP/1.1")
            put("headers", headers(e.requestHeaders))
            put("queryString", JSONArray())
            put("cookies", JSONArray())
            put("headersSize", -1)
            put("bodySize", e.requestBodySize)
            e.requestBody?.let {
                put("postData", JSONObject()
                    .put("mimeType", e.requestContentType ?: "application/octet-stream")
                    .put("text", it))
            }
        })
        put("response", JSONObject().apply {
            put("status", e.responseCode)
            put("statusText", e.responseMessage)
            put("httpVersion", "HTTP/1.1")
            put("headers", headers(e.responseHeaders))
            put("cookies", JSONArray())
            put("content", JSONObject().apply {
                put("size", e.responseBodySize)
                put("mimeType", e.responseContentType ?: "application/octet-stream")
                e.responseBody?.let { put("text", it) }
            })
            put("redirectURL", "")
            put("headersSize", -1)
            put("bodySize", e.responseBodySize)
        })
        put("cache", JSONObject())
        put("timings", JSONObject()
            .put("send", 0)
            .put("wait", e.durationMs)
            .put("receive", 0))
        e.error?.let { put("_error", it) }
    }

    private fun headers(map: Map<String, String>): JSONArray = JSONArray().also { arr ->
        map.forEach { (k, v) -> arr.put(JSONObject().put("name", k).put("value", v)) }
    }
}
