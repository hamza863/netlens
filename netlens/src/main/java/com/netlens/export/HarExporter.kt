package com.netlens.export

import com.netlens.model.NetworkLogEntry
import com.netlens.redact.Redactor
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
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
            put("queryString", queryString(e.url))
            put("cookies", requestCookies(e.requestHeaders))
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
            put("cookies", responseCookies(e.responseHeaders))
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
        map.forEach { (k, v) ->
            arr.put(JSONObject().put("name", k).put("value", Redactor.value(k, v)))
        }
    }

    /** Parse `?a=1&b=2` from the URL into HAR `queryString` name/value pairs. */
    private fun queryString(url: String): JSONArray = JSONArray().also { arr ->
        val query = try { URI(url).rawQuery } catch (_: Exception) { null } ?: return@also
        query.split("&").forEach { pair ->
            if (pair.isEmpty()) return@forEach
            val i = pair.indexOf('=')
            val name = if (i >= 0) pair.substring(0, i) else pair
            val value = if (i >= 0) pair.substring(i + 1) else ""
            arr.put(JSONObject()
                .put("name", decode(name))
                .put("value", decode(value)))
        }
    }

    /** Request `Cookie: a=1; b=2` header → HAR cookies (masked when sensitive). */
    private fun requestCookies(map: Map<String, String>): JSONArray = JSONArray().also { arr ->
        val cookie = header(map, "Cookie") ?: return@also
        if (Redactor.isSensitive("Cookie")) {
            arr.put(JSONObject().put("name", "Cookie").put("value", Redactor.MASK))
            return@also
        }
        cookie.split(";").forEach { pair ->
            val i = pair.indexOf('=')
            if (i < 0) return@forEach
            arr.put(JSONObject()
                .put("name", pair.substring(0, i).trim())
                .put("value", pair.substring(i + 1).trim()))
        }
    }

    /** Response `Set-Cookie` header → HAR cookies (masked when sensitive). */
    private fun responseCookies(map: Map<String, String>): JSONArray = JSONArray().also { arr ->
        val setCookie = header(map, "Set-Cookie") ?: return@also
        if (Redactor.isSensitive("Set-Cookie")) {
            arr.put(JSONObject().put("name", "Set-Cookie").put("value", Redactor.MASK))
            return@also
        }
        // First "name=value" segment is the cookie; the rest are attributes (path, etc.).
        val first = setCookie.split(";").first()
        val i = first.indexOf('=')
        if (i >= 0) {
            arr.put(JSONObject()
                .put("name", first.substring(0, i).trim())
                .put("value", first.substring(i + 1).trim()))
        }
    }

    private fun header(map: Map<String, String>, name: String): String? =
        map.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    private fun decode(s: String): String =
        try { java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }
}
