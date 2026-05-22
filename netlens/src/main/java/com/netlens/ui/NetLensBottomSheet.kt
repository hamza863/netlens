package com.netlens.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.netlens.NetworkLogStore
import com.netlens.model.NetworkLogEntry

class NetLensBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "NetLensBottomSheet"

        fun show(activity: FragmentActivity) {
            val fm = activity.supportFragmentManager
            if (fm.findFragmentByTag(TAG) == null) {
                NetLensBottomSheet().show(fm, TAG)
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onStart() {
        super.onStart()
        // Expand to full height on open
        val bottomSheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state     = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 600
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = buildRootView(requireContext())

    // ── UI Construction ───────────────────────────────────────────────────────

    private fun buildRootView(ctx: Context): View {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        root.addView(buildHandle(ctx))
        root.addView(buildHeader(ctx))
        root.addView(buildSearchBar(ctx) { query -> refreshList(root, ctx, query) })
        root.addView(buildDivider(ctx))

        // List container (tagged so we can refresh it)
        val listContainer = buildLogList(ctx, NetworkLogStore.getAll())
        listContainer.tag = "list_container"
        root.addView(listContainer)

        return root
    }

    private fun buildHandle(ctx: Context) = View(ctx).apply {
        val lp = LinearLayout.LayoutParams(48.dp(ctx), 4.dp(ctx))
        lp.setMargins(0, 10.dp(ctx), 0, 6.dp(ctx))
        lp.gravity = android.view.Gravity.CENTER_HORIZONTAL
        layoutParams = lp
        setBackgroundColor(Color.parseColor("#DDDDDD"))
        // rounded handle
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#CCCCCC"))
            cornerRadius = 8f
        }
    }

    private fun buildHeader(ctx: Context): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16.dp(ctx), 8.dp(ctx), 12.dp(ctx), 4.dp(ctx))
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val title = TextView(ctx).apply {
            text = "🔍 NetLens"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#111827"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val countBadge = TextView(ctx).apply {
            text = "${NetworkLogStore.size()}"
            textSize = 12f
            setTextColor(Color.parseColor("#6B7280"))
            setPadding(8.dp(ctx), 0, 12.dp(ctx), 0)
        }

        val clearBtn = buildTextButton(ctx, "Clear All", "#EF4444") {
            NetworkLogStore.clear()
            dismiss()
        }

        row.addView(title)
        row.addView(countBadge)
        row.addView(clearBtn)
        return row
    }

    private fun buildSearchBar(ctx: Context, onQuery: (String) -> Unit): View {
        val container = LinearLayout(ctx).apply {
            setPadding(12.dp(ctx), 4.dp(ctx), 12.dp(ctx), 8.dp(ctx))
        }

        val editText = EditText(ctx).apply {
            hint = "Search URL, method, status..."
            textSize = 14f
            setTextColor(Color.parseColor("#111827"))
            setHintTextColor(Color.parseColor("#9CA3AF"))
            setPadding(12.dp(ctx), 10.dp(ctx), 12.dp(ctx), 10.dp(ctx))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F3F4F6"))
                cornerRadius = 8.dp(ctx).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = onQuery(s?.toString() ?: "")
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        })

        container.addView(editText)
        return container
    }

    private fun refreshList(root: LinearLayout, ctx: Context, query: String) {
        val old = root.findViewWithTag<View>("list_container") ?: return
        val idx = root.indexOfChild(old)
        root.removeView(old)

        val entries = if (query.isBlank()) NetworkLogStore.getAll()
                      else NetworkLogStore.filter(query)
        val newList = buildLogList(ctx, entries)
        newList.tag = "list_container"
        root.addView(newList, idx)
    }

    private fun buildLogList(ctx: Context, entries: List<NetworkLogEntry>): View {
        val scroll = ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val list = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10.dp(ctx), 4.dp(ctx), 10.dp(ctx), 24.dp(ctx))
        }

        if (entries.isEmpty()) {
            list.addView(TextView(ctx).apply {
                text = "No requests yet.\nMake an API call and shake again."
                setPadding(16.dp(ctx), 32.dp(ctx), 16.dp(ctx), 8.dp(ctx))
                setTextColor(Color.parseColor("#9CA3AF"))
                gravity = android.view.Gravity.CENTER
            })
        } else {
            entries.forEach { list.addView(buildEntryRow(ctx, it)) }
        }

        scroll.addView(list)
        return scroll
    }

    private fun buildEntryRow(ctx: Context, entry: NetworkLogEntry): View {
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.dp(ctx), 10.dp(ctx), 12.dp(ctx), 10.dp(ctx))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F9FAFB"))
                cornerRadius = 8.dp(ctx).toFloat()
                setStroke(1, Color.parseColor("#E5E7EB"))
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 4.dp(ctx), 0, 4.dp(ctx)) }
            layoutParams = lp
            isClickable = true
            isFocusable  = true
            setOnClickListener { showDetailDialog(ctx, entry) }
        }

        // Row 1: method badge + truncated URL
        val topRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        topRow.addView(buildMethodBadge(ctx, entry.method))
        topRow.addView(TextView(ctx).apply {
            text = entry.shortUrl.take(55) + if (entry.shortUrl.length > 55) "…" else ""
            textSize = 13f
            setTextColor(Color.parseColor("#111827"))
            setPadding(8.dp(ctx), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        card.addView(topRow)

        // Row 2: status + duration + time
        val bottomRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 5.dp(ctx), 0, 0)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        bottomRow.addView(buildStatusBadge(ctx, entry))
        bottomRow.addView(buildMeta(ctx, "  ${entry.durationMs}ms", "#6B7280"))
        bottomRow.addView(buildMeta(ctx, "  •  ${entry.formattedTime}", "#9CA3AF"))
        card.addView(bottomRow)

        return card
    }

    // ── Detail Dialog ─────────────────────────────────────────────────────────

    private fun showDetailDialog(ctx: Context, entry: NetworkLogEntry) {
        val text = buildDetailText(entry)

        val scroll = ScrollView(ctx)
        scroll.addView(TextView(ctx).apply {
            this.text = text
            textSize  = 11.5f
            typeface  = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#111827"))
            setPadding(16.dp(ctx), 16.dp(ctx), 16.dp(ctx), 16.dp(ctx))
        })

        AlertDialog.Builder(ctx)
            .setTitle("${entry.method}  ${entry.statusLabel}  (${entry.durationMs}ms)")
            .setView(scroll)
            .setPositiveButton("📋 Copy") { _, _ ->
                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("NetLens", text))
                Toast.makeText(ctx, "Copied!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
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

    // ── Small Builders ────────────────────────────────────────────────────────

    private fun buildMethodBadge(ctx: Context, method: String) = TextView(ctx).apply {
        text = method
        textSize = 10f
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(6.dp(ctx), 2.dp(ctx), 6.dp(ctx), 2.dp(ctx))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(methodColor(method))
            cornerRadius = 4.dp(ctx).toFloat()
        }
    }

    private fun buildStatusBadge(ctx: Context, entry: NetworkLogEntry) = TextView(ctx).apply {
        text = entry.statusLabel
        textSize = 12f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(statusColor(entry))
    }

    private fun buildMeta(ctx: Context, text: String, hex: String) = TextView(ctx).apply {
        this.text = text
        textSize  = 11f
        setTextColor(Color.parseColor(hex))
    }

    private fun buildTextButton(ctx: Context, label: String, colorHex: String, onClick: () -> Unit) =
        TextView(ctx).apply {
            text = label
            textSize = 13f
            setTextColor(Color.parseColor(colorHex))
            setPadding(8.dp(ctx), 6.dp(ctx), 8.dp(ctx), 6.dp(ctx))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

    private fun buildDivider(ctx: Context) = View(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        setBackgroundColor(Color.parseColor("#E5E7EB"))
    }

    // ── Colors ────────────────────────────────────────────────────────────────

    private fun methodColor(method: String) = when (method.uppercase()) {
        "GET"    -> Color.parseColor("#3B82F6")
        "POST"   -> Color.parseColor("#10B981")
        "PUT"    -> Color.parseColor("#F59E0B")
        "PATCH"  -> Color.parseColor("#8B5CF6")
        "DELETE" -> Color.parseColor("#EF4444")
        else     -> Color.parseColor("#6B7280")
    }

    private fun statusColor(entry: NetworkLogEntry) = when {
        entry.isFailed      -> Color.parseColor("#6B7280")
        entry.isSuccess     -> Color.parseColor("#10B981")
        entry.isRedirect    -> Color.parseColor("#F59E0B")
        entry.isClientError -> Color.parseColor("#EF4444")
        entry.isServerError -> Color.parseColor("#8B5CF6")
        else                -> Color.parseColor("#6B7280")
    }

    private fun Int.dp(ctx: Context) = (this * ctx.resources.displayMetrics.density).toInt()
}
