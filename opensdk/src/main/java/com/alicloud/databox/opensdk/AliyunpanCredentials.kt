package com.alicloud.databox.opensdk

import android.content.Context
import org.json.JSONObject

abstract class AliyunpanCredentials(val context: Context, val appId: String) :
    AliyunpanTokenServer {

    abstract fun preCheckTokenValid(): Boolean

    abstract fun clearToken()

    abstract fun getOAuthRequest(scope: String): Map<String, String>

    abstract fun getAccessToken(): String?

    @Throws(Exception::class)
    abstract fun updateAccessToken(jsonObject: JSONObject)
}