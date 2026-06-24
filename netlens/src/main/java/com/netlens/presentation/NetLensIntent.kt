package com.netlens.presentation

import com.netlens.model.NetworkLogEntry

/**
 * User intents for the NetLens viewer (the **Intent** of MVI).
 *
 * Every user action becomes one of these and is dispatched to
 * [NetLensViewModel.onIntent]; nothing else mutates state.
 */
sealed interface NetLensIntent {
    /** Re-read the log store (e.g. after new requests arrive). */
    data object Refresh : NetLensIntent
    data class Search(val query: String) : NetLensIntent
    data class SetFilter(val filter: LogFilter) : NetLensIntent
    data class Select(val entry: NetworkLogEntry) : NetLensIntent
    data object CloseDetail : NetLensIntent
    data object ClearAll : NetLensIntent
}
