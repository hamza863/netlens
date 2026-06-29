package com.netlens.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

/**
 * Interactive, collapsible JSON viewer. Objects and arrays are tap-to-expand
 * rows; deeper levels start collapsed so large payloads stay scannable. Falls
 * back to nothing for non-JSON — callers gate on [parseJsonOrNull] first.
 */

/** Returns a [JSONObject]/[JSONArray] if [raw] is parseable JSON, else null. */
internal fun parseJsonOrNull(raw: String): Any? {
    val t = raw.trim()
    return try {
        when {
            t.startsWith("{") -> JSONObject(t)
            t.startsWith("[") -> JSONArray(t)
            else -> null
        }
    } catch (_: Exception) { null }
}

@Composable
internal fun JsonTreeView(root: Any, modifier: Modifier = Modifier) {
    SelectionContainer {
        Column(modifier = modifier) {
            JsonNode(label = null, value = root, depth = 0)
        }
    }
}

@Composable
private fun JsonNode(label: String?, value: Any?, depth: Int) {
    val isContainer = value is JSONObject || value is JSONArray
    if (isContainer) {
        var expanded by remember { mutableStateOf(depth < 2) }
        val isObject = value is JSONObject
        val children = childrenOf(value)
        val open = if (isObject) "{" else "["
        val close = if (isObject) "}" else "]"

        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(start = (depth * 14).dp, top = 1.dp, bottom = 1.dp)
        ) {
            Mono(if (expanded) "▾ " else "▸ ", COLOR_PUNCT)
            if (label != null) Mono("$label: ", COLOR_KEY)
            Mono(if (expanded) open else "$open ${children.size} $close", COLOR_PUNCT)
        }

        if (expanded) {
            children.forEach { (k, v) -> JsonNode(k, v, depth + 1) }
            Row(modifier = Modifier.padding(start = (depth * 14).dp + 18.dp, top = 1.dp, bottom = 1.dp)) {
                Mono(close, COLOR_PUNCT)
            }
        }
    } else {
        Row(modifier = Modifier.padding(start = (depth * 14).dp + 18.dp, top = 1.dp, bottom = 1.dp)) {
            if (label != null) Mono("$label: ", COLOR_KEY)
            Mono(renderPrimitive(value), colorFor(value))
        }
    }
}

@Composable
private fun Mono(text: String, color: Color) {
    androidx.compose.material3.Text(
        text = text,
        fontSize = 12.5.sp,
        fontFamily = FontFamily.Monospace,
        color = color
    )
}

private fun childrenOf(value: Any?): List<Pair<String, Any?>> = when (value) {
    is JSONObject -> value.keys().asSequence().map { it to value.opt(it) }.toList()
    is JSONArray  -> (0 until value.length()).map { "[$it]" to value.opt(it) }
    else          -> emptyList()
}

private fun renderPrimitive(value: Any?): String = when {
    value == null || value == JSONObject.NULL -> "null"
    value is String                           -> "\"$value\""
    else                                      -> value.toString()
}

private fun colorFor(value: Any?): Color = when {
    value == null || value == JSONObject.NULL -> COLOR_NULL
    value is String                           -> COLOR_STRING
    value is Boolean                          -> COLOR_BOOL
    else                                      -> COLOR_NUMBER
}

private val COLOR_KEY    = Color(0xFF6B7280)
private val COLOR_PUNCT  = Color(0xFF374151)
private val COLOR_STRING = Color(0xFF059669)
private val COLOR_NUMBER = Color(0xFF2563EB)
private val COLOR_BOOL   = Color(0xFFD97706)
private val COLOR_NULL   = Color(0xFFDC2626)
