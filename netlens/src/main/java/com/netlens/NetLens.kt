package com.netlens

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.netlens.shake.ShakeDetector
import com.netlens.ui.NetLensBubble
import com.netlens.ui.NetLensViewer
import com.netlens.ui.NetLensOverlay

/**
 *  ███╗   ██╗███████╗████████╗██╗     ███████╗███╗   ██╗███████╗
 *
 *  Lightweight Android network logger — fully Compose-based.
 *  Shake your device to inspect all API calls. Zero DB. Zero fragments.
 *
 *  ── Quick start ──────────────────────────────────────────────────────────────
 *
 *  1. Add interceptor to OkHttpClient (debug builds only):
 *
 *       OkHttpClient.Builder()
 *           .addInterceptor(NetLens.interceptor())
 *           .build()
 *
 *  2. Install shake-to-open in your Application class:
 *
 *       NetLens.install(this)
 *
 *  3. Or show manually inside any Composable:
 *
 *       var show by remember { mutableStateOf(false) }
 *       if (show) NetLensViewer { show = false }
 *
 *  ─────────────────────────────────────────────────────────────────────────────
 */
object NetLens {

    private var config = NetLensConfig()

    /** Call once in Application.onCreate() to configure and enable shake-to-open. */
    fun install(app: Application, config: NetLensConfig = NetLensConfig()) {
        this.config = config
        NetworkLogStore.maxEntries = config.maxEntries

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private val shakeDetectors = mutableMapOf<Activity, ShakeDetector>()
            private val bubbles = mutableMapOf<Activity, NetLensBubble>()

            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                if (activity !is ComponentActivity) return

                if (config.shakeToOpen) {
                    shakeDetectors.getOrPut(activity) {
                        ShakeDetector(
                            context = activity,
                            threshold = config.shakeThreshold,
                            onShake = { show(activity) }
                        )
                    }.register()
                }

                if (config.showBubble) {
                    bubbles.getOrPut(activity) {
                        NetLensBubble(activity) { show(activity) }
                    }.attach()
                }
            }

            override fun onActivityPaused(activity: Activity) {
                shakeDetectors[activity]?.unregister()
                bubbles[activity]?.detach()
            }

            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                shakeDetectors.remove(activity)
                bubbles.remove(activity)?.detach()
            }
        })
    }

    /** Returns the OkHttp interceptor. Add this to your OkHttpClient. */
    fun interceptor(): NetLensInterceptor = NetLensInterceptor(
        maxBodyBytes = config.maxBodyBytes
    )

    /**
     * Programmatically show the log viewer over the given activity.
     * Uses a Compose overlay — no fragments, no AppCompat needed.
     */
    fun show(activity: ComponentActivity) {
        NetLensOverlay.show(activity)
    }

    /** Clear all stored logs. */
    fun clear() = NetworkLogStore.clear()

    /** How many entries are currently stored. */
    fun logCount(): Int = NetworkLogStore.size()
}

/**
 * Configuration for NetLens.
 */
data class NetLensConfig(
    val shakeToOpen: Boolean  = true,
    val maxEntries: Int       = 200,
    val maxBodyBytes: Long    = 64 * 1024L,
    val shakeThreshold: Float = 12f,
    /** Show a draggable floating button to open the viewer (handy on emulators). */
    val showBubble: Boolean   = false
)
