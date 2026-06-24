package com.netlens.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netlens.NetworkLogStore
import com.netlens.model.NetworkLogEntry

/**
 * MVI store for the NetLens viewer.
 *
 * Holds the single source of truth ([state]) and is the only place that mutates
 * it, always in response to a [NetLensIntent]. Implemented with Compose snapshot
 * state so it needs no `androidx.lifecycle` dependency — keeping NetLens
 * dependency-free — while staying a plain, unit-testable class.
 *
 * Reads from [NetworkLogStore] (the data source); the store stays the system of
 * record, this layer only projects it into view state.
 */
class NetLensViewModel {

    var state by mutableStateOf(NetLensState())
        private set

    init { recompute() }

    fun onIntent(intent: NetLensIntent) {
        when (intent) {
            is NetLensIntent.Refresh     -> recompute()
            is NetLensIntent.Search      -> { state = state.copy(query = intent.query); recompute() }
            is NetLensIntent.SetFilter   -> { state = state.copy(filter = intent.filter); recompute() }
            is NetLensIntent.Select       -> state = state.copy(selected = intent.entry)
            is NetLensIntent.CloseDetail  -> state = state.copy(selected = null)
            is NetLensIntent.ClearAll     -> { NetworkLogStore.clear(); recompute() }
        }
    }

    /** Re-derive [NetLensState.visibleEntries] and stats from the current store snapshot. */
    private fun recompute() {
        val all = NetworkLogStore.getAll()
        val visible = all.filter { entry ->
            state.filter.matches(entry) &&
                (state.query.isBlank() || matchesQuery(entry, state.query))
        }
        state = state.copy(visibleEntries = visible, stats = statsOf(all))
    }

    private fun matchesQuery(entry: NetworkLogEntry, query: String): Boolean {
        val q = query.lowercase().trim()
        return entry.url.lowercase().contains(q) ||
               entry.method.lowercase().contains(q) ||
               entry.statusLabel.lowercase().contains(q)
    }

    private fun statsOf(all: List<NetworkLogEntry>): NetLensState.Stats {
        if (all.isEmpty()) return NetLensState.Stats()
        val failed = all.count { it.isFailed || it.isClientError || it.isServerError }
        val avg = all.map { it.durationMs }.average().toLong()
        return NetLensState.Stats(total = all.size, failed = failed, avgMs = avg)
    }
}
