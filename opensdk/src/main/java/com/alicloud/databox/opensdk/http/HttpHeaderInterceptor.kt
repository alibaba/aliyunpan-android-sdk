package com.alicloud.databox.opensdk.http

import okhttp3.Interceptor
import okhttp3.Response

class HttpHeaderInterceptor(private val config: HttpHeaderConfig) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
            .apply {
                addHeader(HEADER_USER_AGENT, config.getConfigUserAgent())
                val accessToken = config.getConfigAuthorization()
                if (!accessToken.isNullOrEmpty()) {
                    addHeader(HEADER_AUTHORIZATION, accessToken)
                }
            }

        return chain.proceed(builder.build())
    }

    interface HttpHeaderConfig {

        fun getConfigUserAgent(): String
        fun getConfigAuthorization(): String?
    }

    companion object {

        const val HEADER_USER_AGENT = "User-Agent"
        const val HEADER_AUTHORIZATION = "Authorization"
    }
}