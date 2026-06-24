package com.netlens.presentation

import com.netlens.model.NetworkLogEntry

/**
 * Immutable UI state for the NetLens viewer (the **Model** of MVI).
 *
 * The single source of truth rendered by the view. Produced only by
 * [NetLensViewModel] in response to [NetLensIntent]s — the view never mutates it.
 */
data class NetLensState(
    val query: String = "",
    val filter: LogFilter = LogFilter.ALL,
    /** The entries to display, already filtered by [query] and [filter]. */
    val visibleEntries: List<NetworkLogEntry> = emptyList(),
    /** Entry whose detail screen is open, or null for the list. */
    val selected: NetworkLogEntry? = null,
    val stats: Stats = Stats()
) {
    data class Stats(
        val total: Int = 0,
        val failed: Int = 0,
        val avgMs: Long = 0L
    ) {
        val summary: String
            get() = if (total == 0) "No requests yet"
                    else "$total calls · $failed failed · avg ${avgMs}ms"
    }
}
