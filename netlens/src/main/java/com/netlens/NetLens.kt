package com.netlens

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.netlens.shake.ShakeDetector
import com.netlens.ui.NetLensBottomSheet

/**
 *  ███╗   ██╗███████╗████████╗██╗     ███████╗███╗   ██╗███████╗
 *  ████╗  ██║██╔════╝╚══██╔══╝██║     ██╔════╝████╗  ██║██╔════╝
 *  ██╔██╗ ██║█████╗     ██║   ██║     █████╗  ██╔██╗ ██║███████╗
 *  ██║╚██╗██║██╔══╝     ██║   ██║     ██╔══╝  ██║╚██╗██║╚════██║
 *  ██║ ╚████║███████╗   ██║   ███████╗███████╗██║ ╚████║███████║
 *
 *  Lightweight Android network logger.
 *  Shake your device to inspect all API calls in a Netfox-style bottom sheet.
 *  Zero DB. Zero background threads. Zero impact on your API speed.
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
 *       NetLens.install(this)        // registers for every activity automatically
 *
 *  3. Or open manually from anywhere:
 *
 *       NetLens.show(activity)
 *
 *  ─────────────────────────────────────────────────────────────────────────────
 */
object NetLens {

    // ── Configuration ─────────────────────────────────────────────────────────

    private var config = NetLensConfig()

    /** Call once in Application.onCreate() to configure and enable shake-to-open. */
    fun install(app: Application, config: NetLensConfig = NetLensConfig()) {
        this.config = config
        NetworkLogStore.maxEntries = config.maxEntries

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private val shakeDetectors = mutableMapOf<Activity, ShakeDetector>()

            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                if (!config.shakeToOpen) return
                if (activity !is FragmentActivity) return
                shakeDetectors.getOrPut(activity) {
                    ShakeDetector(activity) { show(activity) }
                }.register()
            }

            override fun onActivityPaused(activity: Activity) {
                shakeDetectors[activity]?.unregister()
            }

            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                shakeDetectors.remove(activity)
            }
        })
    }

    /** Returns the OkHttp interceptor. Add this to your OkHttpClient. */
    fun interceptor(): NetLensInterceptor = NetLensInterceptor(
        maxBodyBytes = config.maxBodyBytes
    )

    /** Programmatically open the log viewer bottom sheet. */
    fun show(activity: FragmentActivity) {
        NetLensBottomSheet.show(activity)
    }

    /** Clear all stored logs. */
    fun clear() = NetworkLogStore.clear()

    /** How many entries are currently stored. */
    fun logCount(): Int = NetworkLogStore.size()
}

/**
 * Configuration for NetLens.
 *
 * @param shakeToOpen   Enable shake gesture to open the log viewer. Default: true.
 * @param maxEntries    Max number of log entries kept in memory. Default: 200.
 * @param maxBodyBytes  Max bytes to read from request/response body. Default: 64 KB.
 * @param shakeThreshold Sensitivity of the shake gesture (m/s² above gravity). Default: 12f.
 */
data class NetLensConfig(
    val shakeToOpen: Boolean  = true,
    val maxEntries: Int       = 200,
    val maxBodyBytes: Long    = 64 * 1024L,
    val shakeThreshold: Float = 12f
)
