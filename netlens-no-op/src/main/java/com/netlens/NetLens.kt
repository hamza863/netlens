package com.netlens

import android.app.Application
import androidx.activity.ComponentActivity

/** No-op implementation for release builds. */
object NetLens {
    fun install(app: Application, config: NetLensConfig = NetLensConfig()) = Unit
    fun interceptor(): NetLensInterceptor = NetLensInterceptor()
    fun show(activity: ComponentActivity) = Unit
    fun clear() = Unit
    fun logCount(): Int = 0
}

data class NetLensConfig(
    val shakeToOpen: Boolean  = true,
    val maxEntries: Int       = 200,
    val maxBodyBytes: Long    = 64 * 1024L,
    val shakeThreshold: Float = 12f,
    val showBubble: Boolean   = false,
    val redactHeaders: Set<String> = setOf(
        "authorization", "proxy-authorization", "cookie", "set-cookie",
        "x-api-key", "x-auth-token", "x-access-token", "x-csrf-token"
    )
)
