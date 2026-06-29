package com.netlens

import android.app.Application
import okhttp3.OkHttpClient

/**
 * Sample app entry point. Builds a single [OkHttpClient] wired with the NetLens
 * interceptor and installs the shake / bubble triggers.
 *
 * This mirrors exactly what a real consumer does in their own Application class.
 */
class DemoApp : Application() {

    /** Shared client — also handed to Retrofit in a real app. */
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(NetLens.interceptor())
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        NetLens.install(
            app = this,
            config = NetLensConfig(
                shakeToOpen = true,
                showBubble = true, // draggable button — easy to demo on an emulator
            )
        )
    }
}
