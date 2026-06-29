package com.netlens.redact

/**
 * Masks sensitive header (and cookie) values before a captured call leaves the
 * device — i.e. in **cURL**, **HAR** and **shared/copied** text. The on-screen
 * viewer still shows the real values, since you're inspecting your own debug
 * build; redaction only protects the artifacts you hand to someone else.
 *
 * The set of header names treated as sensitive is configurable via
 * `NetLensConfig.redactHeaders` and applied here from `NetLens.install`.
 */
object Redactor {

    /** Headers masked by default. Matched case-insensitively. */
    val DEFAULT_HEADERS: Set<String> = setOf(
        "authorization",
        "proxy-authorization",
        "cookie",
        "set-cookie",
        "x-api-key",
        "x-auth-token",
        "x-access-token",
        "x-csrf-token"
    )

    const val MASK: String = "██ REDACTED ██"

    /** Lower-cased for case-insensitive matching; set once from `NetLens.install`. */
    @Volatile
    var headers: Set<String> = DEFAULT_HEADERS

    fun isSensitive(name: String): Boolean = name.lowercase() in headers

    /** Returns [value] unchanged, or [MASK] if [name] is a sensitive header. */
    fun value(name: String, value: String): String =
        if (isSensitive(name)) MASK else value

    /** Copy of [map] with the values of sensitive headers masked. */
    fun headers(map: Map<String, String>): Map<String, String> =
        map.mapValues { (k, v) -> value(k, v) }
}
