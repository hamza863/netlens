package com.netlens

import okhttp3.Interceptor
import okhttp3.Response

/** No-op interceptor — passes calls through untouched. */
class NetLensInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
