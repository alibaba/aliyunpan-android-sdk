package com.alicloud.databox.opensdk

import org.json.JSONObject

interface AliyunpanTokenServer {

    /**
     * Get token request
     *
     * @param authCode 客户端授权回调的code
     * @return 获取token的请求JsonObject 同步方式
     */
    fun getTokenRequest(authCode: String): JSONObject? {
        return null
    }

    /**
     * Get token
     *
     * @param authCode authCode 客户端授权回调的code
     * @param onResult 异步回调的token结果 JsonObject结果
     */
    fun getToken(authCode: String, onResult: Consumer<JSONObject?>) {
        onResult.accept(null)
    }

    /**
     * Get oauth qrcode request
     *
     * @param scopes 申请的授权范围
     * @return 获取授权二维码的请求JsonObject 同步方式
     */
    fun getOAuthQRCodeRequest(scopes: List<String>): JSONObject? {
        return null
    }

    /**
     * Get oauth qrcode request
     *
     * @param scopes 申请的授权范围
     * @param onResult 异步回调的获取授权二维码的请求JsonObject
     */
    fun getOAuthQRCodeRequest(scopes: List<String>, onResult: Consumer<JSONObject?>) {
        onResult.accept(null)
    }
}