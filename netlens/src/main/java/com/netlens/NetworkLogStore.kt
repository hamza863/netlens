package com.netlens

import com.netlens.model.NetworkLogEntry
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe in-memory circular buffer. No database. No disk I/O.
 * Automatically drops the oldest entries once [maxEntries] is reached.
 */
object NetworkLogStore {

    var maxEntries: Int = 200
    private val _logs = CopyOnWriteArrayList<NetworkLogEntry>()

    /** Add a new entry (newest-first order). */
    fun add(entry: NetworkLogEntry) {
        _logs.add(0, entry)
        while (_logs.size > maxEntries) {
            _logs.removeAt(_logs.lastIndex)
        }
    }

    /** All stored entries, newest first. */
    fun getAll(): List<NetworkLogEntry> = _logs.toList()

    /**
     * Filter by URL, method, status, or anything in the headers/bodies.
     * e.g. filter("users"), filter("POST"), filter("4") → all 4xx,
     * filter("authorization"), filter("error-code-123") → body match.
     */
    fun filter(query: String): List<NetworkLogEntry> {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return _logs.toList()
        return _logs.filter { entry ->
            entry.url.lowercase().contains(q) ||
            entry.method.lowercase().contains(q) ||
            entry.statusLabel.lowercase().contains(q) ||
            entry.requestBody?.lowercase()?.contains(q) == true ||
            entry.responseBody?.lowercase()?.contains(q) == true ||
            entry.requestHeaders.any { (k, v) -> k.lowercase().contains(q) || v.lowercase().contains(q) } ||
            entry.responseHeaders.any { (k, v) -> k.lowercase().contains(q) || v.lowercase().contains(q) }
        }
    }

    fun clear() = _logs.clear()

    fun size() = _logs.size
}
