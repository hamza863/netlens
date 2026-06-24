package com.netlens.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.netlens.export.NetLensShare
import com.netlens.model.NetworkLogEntry
import com.netlens.presentation.LogFilter
import com.netlens.presentation.NetLensIntent
import com.netlens.presentation.NetLensState
import com.netlens.presentation.NetLensViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader
import java.io.StringWriter
import java.net.URLDecoder
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

// ── Public entry point ────────────────────────────────────────────────────────

/**
 * Call this from any Composable to show the NetLens log viewer as a full-screen dialog.
 *
 * The view is driven by [NetLensViewModel] (MVI): it renders [NetLensState] and
 * forwards user actions as [NetLensIntent]s. It owns no business logic itself.
 *
 * Example:
 *   var showNetLens by remember { mutableStateOf(false) }
 *   if (showNetLens) NetLensViewer { showNetLens = false }
 */
@Composable
fun NetLensViewer(onDismiss: () -> Unit) {
    val viewModel = remember { NetLensViewModel() }
    val state = viewModel.state
    val onIntent: (NetLensIntent) -> Unit = viewModel::onIntent
    val context = LocalContext.current

    // Detail screen
    state.selected?.let { entry ->
        DetailDialog(entry = entry, onDismiss = { onIntent(NetLensIntent.CloseDetail) })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = Color(0xFFFFFFFF),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Grabber()

                Header(
                    stats = state.stats,
                    onExport = { NetLensShare.shareHar(context, state.visibleEntries) },
                    onClear = { onIntent(NetLensIntent.ClearAll) },
                    onClose = onDismiss
                )

                HorizontalDivider(color = Color(0xFFE5E7EB))

                SearchBar(
                    query = state.query,
                    onQueryChange = { onIntent(NetLensIntent.Search(it)) }
                )

                FilterChips(
                    selected = state.filter,
                    onSelect = { onIntent(NetLensIntent.SetFilter(it)) }
                )

                HorizontalDivider(color = Color(0xFFE5E7EB))

                if (state.visibleEntries.isEmpty()) {
                    EmptyState(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.visibleEntries, key = { it.id }) { entry ->
                            EntryRow(entry = entry, onClick = { onIntent(NetLensIntent.Select(entry)) })
                        }
                    }
                }
            }
        }
    }
}

// ── Top of sheet ──────────────────────────────────────────────────────────────

@Composable
private fun Grabber() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(Color(0xFFCCCCCC), RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun Header(
    stats: NetLensState.Stats,
    onExport: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🔍 NetLens",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Export",
                fontSize = 13.sp,
                color = Color(0xFF2563EB),
                modifier = Modifier.clickable(onClick = onExport).padding(6.dp)
            )
            Text(
                text = "Clear",
                fontSize = 13.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.clickable(onClick = onClear).padding(6.dp)
            )
            IconButton(onClick = onClose) {
                Text(text = "✕", fontSize = 18.sp, color = Color(0xFF6B7280))
            }
        }
        Text(
            text = stats.summary,
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}

@Composable
private fun FilterChips(selected: LogFilter, onSelect: (LogFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LogFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(filter.label, fontSize = 12.sp) }
            )
        }
    }
}

// ── Search bar ────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text("Search URL, method, status...", fontSize = 14.sp, color = Color(0xFF9CA3AF))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Color(0xFF6B7280),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor   = Color(0xFFF3F4F6),
                unfocusedContainerColor = Color(0xFFF3F4F6)
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = Color(0xFF111827))
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "No requests yet.\nMake an API call and shake again.",
            color = Color(0xFF9CA3AF),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ── Entry row ─────────────────────────────────────────────────────────────────

@Composable
private fun EntryRow(entry: NetworkLogEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF9FAFB),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MethodBadge(method = entry.method)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.shortUrl,
                    fontSize = 13.sp,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusText(entry = entry)
                Text("  ${entry.durationMs}ms", fontSize = 11.sp, color = Color(0xFF6B7280))
                if (entry.responseBodySize > 0) {
                    Text("  •  ${entry.formattedRespSize}", fontSize = 11.sp, color = Color(0xFF6B7280))
                }
                Text("  •  ${entry.formattedTime}", fontSize = 11.sp, color = Color(0xFF9CA3AF))
            }
        }
    }
}

@Composable
private fun MethodBadge(method: String) {
    val bgColor = when (method.uppercase()) {
        "GET"    -> Color(0xFF3B82F6)
        "POST"   -> Color(0xFF10B981)
        "PUT"    -> Color(0xFFF59E0B)
        "PATCH"  -> Color(0xFF8B5CF6)
        "DELETE" -> Color(0xFFEF4444)
        else     -> Color(0xFF6B7280)
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(method, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusText(entry: NetworkLogEntry) {
    val color = when {
        entry.isFailed      -> Color(0xFF6B7280)
        entry.isSuccess     -> Color(0xFF10B981)
        entry.isRedirect    -> Color(0xFFF59E0B)
        entry.isClientError -> Color(0xFFEF4444)
        entry.isServerError -> Color(0xFF8B5CF6)
        else                -> Color(0xFF6B7280)
    }
    Text(entry.statusLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
}

// ── Detail screen ─────────────────────────────────────────────────────────────

@Composable
private fun DetailDialog(entry: NetworkLogEntry, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val detailText = remember(entry.id) { buildDetailText(entry) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f),
            color = Color(0xFFFFFFFF)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Text("←", fontSize = 20.sp, color = Color(0xFF111827))
                    }
                    Text(
                        text = "${entry.method}  ${entry.statusLabel}  •  ${entry.durationMs}ms",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        copyToClipboard(context, "NetLens cURL", entry.toCurl())
                        Toast.makeText(context, "cURL copied!", Toast.LENGTH_SHORT).show()
                    }) { Text("cURL", fontSize = 12.sp) }
                    TextButton(onClick = { NetLensShare.shareText(context, detailText) }) {
                        Text("Share", fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        copyToClipboard(context, "NetLens", detailText)
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                    }) { Text("📋", fontSize = 14.sp) }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    SectionLabel("REQUEST")
                    KeyValue("URL", entry.url)
                    Spacer(Modifier.height(8.dp))
                    HeadersBlock(entry.requestHeaders)
                    BodyBlock("Request Body", entry.requestBody, entry.requestContentType, entry.requestBodySize)

                    Spacer(Modifier.height(20.dp))

                    if (entry.isFailed) {
                        SectionLabel("ERROR")
                        CodeBlock(entry.error ?: "Unknown error")
                    } else {
                        SectionLabel("RESPONSE")
                        KeyValue("Status", "${entry.responseCode} ${entry.responseMessage}")
                        Spacer(Modifier.height(8.dp))
                        HeadersBlock(entry.responseHeaders)
                        ResponseBody(entry)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ── Detail building blocks ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2563EB),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun KeyValue(key: String, value: String) {
    Text(key, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280))
    SelectionContainer {
        Text(value, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF111827))
    }
}

@Composable
private fun HeadersBlock(headers: Map<String, String>) {
    if (headers.isEmpty()) return
    Text(
        "Headers",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF6B7280),
        modifier = Modifier.padding(bottom = 2.dp)
    )
    SelectionContainer {
        Column {
            headers.forEach { (k, v) ->
                Text("$k: $v", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF374151))
            }
        }
    }
}

private const val BODY_PREVIEW_LIMIT = 4000

@Composable
private fun BodyBlock(label: String, body: String?, contentType: String?, size: Long) {
    if (body.isNullOrBlank()) {
        if (size > 0) {
            Spacer(Modifier.height(10.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280))
            CodeBlock("(${contentType ?: "binary"}, ${NetworkLogEntry.formatBytes(size)})")
        }
        return
    }

    val pretty = remember(body) { prettyBody(body, contentType) }
    val isLarge = pretty.length > BODY_PREVIEW_LIMIT
    var expanded by remember(body) { mutableStateOf(false) }

    Spacer(Modifier.height(10.dp))
    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280), modifier = Modifier.padding(bottom = 2.dp))

    val shown = if (isLarge && !expanded) pretty.take(BODY_PREVIEW_LIMIT) + "\n…" else pretty
    CodeBlock(shown)

    if (isLarge) {
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (expanded) "Show less" else "View full body (${pretty.length} chars)",
                fontSize = 12.sp,
                color = Color(0xFF2563EB)
            )
        }
    }
}

@Composable
private fun ResponseBody(entry: NetworkLogEntry) {
    val bytes = entry.responseImageBytes
    if (bytes != null) {
        Spacer(Modifier.height(10.dp))
        Text("Response Body (image)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280))
        val bitmap = remember(entry.id) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Response image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                    .padding(4.dp)
            )
        } else {
            CodeBlock("Image (${entry.responseContentType}, ${entry.formattedRespSize})")
        }
        return
    }
    if (entry.responseBody.isNullOrBlank() && entry.isImageResponse) {
        Spacer(Modifier.height(10.dp))
        Text("Response Body", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280))
        CodeBlock("Image too large to preview (${entry.responseContentType}, ${entry.formattedRespSize})")
        return
    }
    BodyBlock("Response Body", entry.responseBody, entry.responseContentType, entry.responseBodySize)
}

@Composable
private fun CodeBlock(text: String) {
    SelectionContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text(text, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF111827))
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────────

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun buildDetailText(entry: NetworkLogEntry) = buildString {
    appendLine("━━ REQUEST ━━━━━━━━━━━━━━━━━━━━━━━━━━")
    appendLine("${entry.method} ${entry.url}")
    appendLine()
    if (entry.requestHeaders.isNotEmpty()) {
        appendLine("Headers:")
        entry.requestHeaders.forEach { (k, v) -> appendLine("  $k: $v") }
    }
    if (!entry.requestBody.isNullOrBlank()) {
        appendLine()
        appendLine("Body:")
        appendLine(prettyBody(entry.requestBody, entry.requestContentType))
    }
    appendLine()
    if (entry.isFailed) {
        appendLine("━━ ERROR ━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine(entry.error)
    } else {
        appendLine("━━ RESPONSE ━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("${entry.responseCode} ${entry.responseMessage}  (${entry.durationMs}ms)")
        appendLine()
        if (entry.responseHeaders.isNotEmpty()) {
            appendLine("Headers:")
            entry.responseHeaders.forEach { (k, v) -> appendLine("  $k: $v") }
        }
        if (!entry.responseBody.isNullOrBlank()) {
            appendLine()
            appendLine("Body:")
            appendLine(prettyBody(entry.responseBody, entry.responseContentType))
        }
    }
}

/** Pretty-print by content type: JSON, XML and form-urlencoded; otherwise returned trimmed. */
private fun prettyBody(raw: String, contentType: String?): String {
    val ct = contentType?.lowercase() ?: ""
    val t = raw.trim()
    return when {
        ct.contains("json") || t.startsWith("{") || t.startsWith("[") -> prettyJson(t)
        ct.contains("xml") || t.startsWith("<") -> prettyXml(t)
        ct.contains("form-urlencoded") -> prettyForm(t)
        else -> t
    }
}

private fun prettyJson(raw: String): String = try {
    when {
        raw.startsWith("{") -> JSONObject(raw).toString(2)
        raw.startsWith("[") -> JSONArray(raw).toString(2)
        else -> raw
    }
} catch (_: Exception) { raw }

private fun prettyXml(raw: String): String = try {
    val out = StringWriter()
    TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    }.transform(StreamSource(StringReader(raw)), StreamResult(out))
    out.toString().trim()
} catch (_: Exception) { raw }

private fun prettyForm(raw: String): String = try {
    raw.split("&").joinToString("\n") { pair ->
        val i = pair.indexOf('=')
        if (i >= 0) {
            val k = URLDecoder.decode(pair.substring(0, i), "UTF-8")
            val v = URLDecoder.decode(pair.substring(i + 1), "UTF-8")
            "$k = $v"
        } else pair
    }
} catch (_: Exception) { raw }
