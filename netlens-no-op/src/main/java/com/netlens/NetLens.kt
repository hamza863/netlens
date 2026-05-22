package com.netlens

import android.app.Application
import okhttp3.Interceptor
import okhttp3.Response

/** Release no-op. All calls are empty. Safe to leave in production code. */
object NetLens {
    fun install(app: Application, config: NetLensConfig = NetLensConfig()) = Unit
    fun interceptor(): Interceptor = Interceptor { it.proceed(it.request()) }
    fun show(activity: Any) = Unit
    fun clear() = Unit
    fun logCount(): Int = 0
}

data class NetLensConfig(
    val shakeToOpen: Boolean  = true,
    val maxEntries: Int       = 200,
    val maxBodyBytes: Long    = 65536L,
    val shakeThreshold: Float = 12f
)
