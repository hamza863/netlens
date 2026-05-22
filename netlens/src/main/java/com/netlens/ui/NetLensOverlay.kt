package com.netlens.ui

import android.app.Dialog
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

/**
 * Shows [NetLensViewer] as a Compose dialog overlay on top of any [ComponentActivity].
 * No fragments. No AppCompat. Works with pure Compose activities.
 */
internal object NetLensOverlay {

    fun show(activity: ComponentActivity) {
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                NetLensViewer(onDismiss = { dialog.dismiss() })
            }
        }

        dialog.setContentView(composeView)
        dialog.show()
    }
}
