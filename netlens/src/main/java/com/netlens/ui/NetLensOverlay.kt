package com.netlens.ui

import android.app.Dialog
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Shows [NetLensViewer] as a Compose dialog overlay on top of any [ComponentActivity].
 * No fragments. No AppCompat. Works with pure Compose activities.
 */
internal object NetLensOverlay {

    /**
     * Reference to the currently visible dialog. Guards against the viewer
     * opening twice when two shake events (or two activities) fire in quick
     * succession.
     */
    private var current: Dialog? = null

    fun show(activity: ComponentActivity) {

        // Already showing? Ignore the duplicate request.
        if (current?.isShowing == true) return

        val dialog = Dialog(
            activity,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )

        dialog.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        val composeView = ComposeView(activity).apply {

            // IMPORTANT FIX
            setViewTreeLifecycleOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)

            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindow
            )

            setContent {
                NetLensViewer(
                    onDismiss = {
                        dialog.dismiss()
                    }
                )
            }
        }

        dialog.setContentView(composeView)
        dialog.setOnDismissListener { if (current === dialog) current = null }
        current = dialog
        dialog.show()
    }
}
