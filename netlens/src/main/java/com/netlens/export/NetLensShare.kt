package com.netlens.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.netlens.model.NetworkLogEntry
import java.io.File

/**
 * Android share-sheet helpers: plain text (cURL / single entry) and a HAR file
 * for the whole session. The HAR is written to the app cache and shared through a
 * [FileProvider] declared in the library manifest, so no host setup is required.
 */
internal object NetLensShare {

    private fun authority(context: Context) = "${context.packageName}.netlens.fileprovider"

    /** Share arbitrary text (e.g. a cURL command or a formatted entry). */
    fun shareText(context: Context, text: String, subject: String = "NetLens") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    /** Export every entry as a `.har` file and open the share sheet. */
    fun shareHar(context: Context, entries: List<NetworkLogEntry>) {
        if (entries.isEmpty()) {
            Toast.makeText(context, "Nothing to export", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val dir = File(context.cacheDir, "netlens").apply { mkdirs() }
            val file = File(dir, "netlens-export.har")
            file.writeText(HarExporter.toHar(entries))

            val uri = FileProvider.getUriForFile(context, authority(context), file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "NetLens export (${entries.size} calls)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export HAR"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
