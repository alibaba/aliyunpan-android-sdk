package com.alicloud.databox.opensdk

import org.json.JSONObject

interface AliyunpanTokenServer {

    /**
     * Get token request
     *
     * @param authCode
     * @return 获取token的请求JsonObject
     */
    fun getTokenRequest(authCode: String): JSONObject? {
        return null
    }

    /**
     * Get token
     *
     * @param authCode
     * @param onResult 异步回调的token结果 JsonObject结果
     */
    fun getToken(authCode: String, onResult: Consumer<JSONObject?>) {
        onResult.accept(null)
    }
}