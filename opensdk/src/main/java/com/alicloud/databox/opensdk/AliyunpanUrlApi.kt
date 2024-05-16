package com.alicloud.databox.opensdk

import okhttp3.HttpUrl

interface AliyunpanUrlApi {

    fun builder(): HttpUrl.Builder

    fun buildUrl(segments: String): HttpUrl

    fun buildUrl(segments: String, queryMap: Map<String, String>): HttpUrl

    companion object {

        private const val DEFAULT_API = BuildConfig.BASE_URL

        private const val DEFAULT_SCHEME = "https"

        fun getUriApi(): AliyunpanUrlApi {
            return object : AliyunpanUrlApi {
                override fun builder(): HttpUrl.Builder {
                    return HttpUrl.Builder()
                        .scheme(DEFAULT_SCHEME)
                        .host(DEFAULT_API)
                }

                override fun buildUrl(segments: String): HttpUrl {
                    return HttpUrl.Builder()
                        .scheme(DEFAULT_SCHEME)
                        .host(DEFAULT_API)
                        .addPathSegments(segments)
                        .build()
                }

                override fun buildUrl(segments: String, queryMap: Map<String, String>): HttpUrl {
                    return HttpUrl.Builder()
                        .scheme(DEFAULT_SCHEME)
                        .host(DEFAULT_API)
                        .addPathSegments(segments).apply {
                            for (entry in queryMap) {
                                addQueryParameter(entry.key, entry.value)
                            }
                        }
                        .build()
                }
            }
        }
    }
}