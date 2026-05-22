package com.netlens.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.netlens.NetworkLogStore
import com.netlens.model.NetworkLogEntry

// ── Public entry point ────────────────────────────────────────────────────────

/**
 * Call this from any Composable to show the NetLens log viewer as a full-screen dialog.
 *
 * Example:
 *   var showNetLens by remember { mutableStateOf(false) }
 *   if (showNetLens) NetLensViewer { showNetLens = false }
 */
@Composable
fun NetLensViewer(onDismiss: () -> Unit) {
    var selectedEntry by remember { mutableStateOf<NetworkLogEntry?>(null) }
    var searchQuery   by remember { mutableStateOf("") }
    var logVersion    by remember { mutableIntStateOf(0) }

    val entries = remember(searchQuery, logVersion) {
        if (searchQuery.isBlank()) NetworkLogStore.getAll()
        else NetworkLogStore.filter(searchQuery)
    }

    // Detail dialog
    selectedEntry?.let { entry ->
        DetailDialog(entry = entry, onDismiss = { selectedEntry = null })
    }

    // Main bottom-sheet style dialog
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
                // Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color(0xFFCCCCCC), RoundedCornerShape(2.dp))
                    )
                }

                // Header
                Header(
                    count = entries.size,
                    onClear = {
                        NetworkLogStore.clear()
                        logVersion++
                        onDismiss()
                    }
                )

                HorizontalDivider(color = Color(0xFFE5E7EB))

                // Search
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                HorizontalDivider(color = Color(0xFFE5E7EB))

                // Log list
                if (entries.isEmpty()) {
                    EmptyState(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            EntryRow(entry = entry, onClick = { selectedEntry = entry })
                        }
                    }
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun Header(count: Int, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🔍 NetLens",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count",
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = "Clear All",
            fontSize = 13.sp,
            color = Color(0xFFEF4444),
            modifier = Modifier
                .clickable(onClick = onClear)
                .padding(6.dp)
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF9FAFB),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // Method + URL row
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
            // Status + duration + time
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusText(entry = entry)
                Text(
                    text = "  ${entry.durationMs}ms",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "  •  ${entry.formattedTime}",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
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
        Text(
            text = method,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
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
    Text(
        text = entry.statusLabel,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

// ── Detail dialog ─────────────────────────────────────────────────────────────

@Composable
private fun DetailDialog(entry: NetworkLogEntry, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val detailText = remember(entry) { buildDetailText(entry) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${entry.method}  ${entry.statusLabel}  (${entry.durationMs}ms)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = detailText,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF111827)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("NetLens", detailText))
                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
            }) { Text("📋 Copy") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        shape = RoundedCornerShape(12.dp)
    )
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
        appendLine(entry.requestBody)
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
            appendLine(entry.responseBody)
        }
    }
}
