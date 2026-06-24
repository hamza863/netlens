package com.netlens.presentation

import com.netlens.model.NetworkLogEntry

/**
 * Quick status filters shown as chips above the log list.
 */
enum class LogFilter(val label: String) {
    ALL("All"),
    SUCCESS("2xx"),
    REDIRECT("3xx"),
    CLIENT("4xx"),
    SERVER("5xx"),
    FAILED("Failed");

    fun matches(entry: NetworkLogEntry): Boolean = when (this) {
        ALL      -> true
        SUCCESS  -> entry.isSuccess
        REDIRECT -> entry.isRedirect
        CLIENT   -> entry.isClientError
        SERVER   -> entry.isServerError
        FAILED   -> entry.isFailed
    }
}
