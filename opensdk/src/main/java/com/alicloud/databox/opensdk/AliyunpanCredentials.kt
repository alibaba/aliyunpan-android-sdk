package com.alicloud.databox.opensdk

import android.content.Context
import okhttp3.HttpUrl
import okhttp3.Request
import org.json.JSONObject

abstract class AliyunpanCredentials(val context: Context, val appId: String, private val baseAuthApi: String) :
    AliyunpanTokenServer {

    abstract fun preCheckTokenValid(): Boolean

    abstract fun clearToken()

    abstract fun getOAuthRequest(scope: String): Request

    abstract fun getAccessToken(): String?

    @Throws(Exception::class)
    abstract fun updateAccessToken(jsonObject: JSONObject)

    fun buildUrl(segments: String): HttpUrl {
        return HttpUrl.Builder()
            .scheme("https")
            .host(baseAuthApi)
            .addPathSegments(segments)
            .build()
    }

    fun buildUrl(segments: String, queryMap: Map<String, String>): HttpUrl {
        return HttpUrl.Builder()
            .scheme("https")
            .host(baseAuthApi)
            .addPathSegments(segments).apply {
                for (entry in queryMap) {
                    addQueryParameter(entry.key, entry.value)
                }
            }
            .build()
    }
}