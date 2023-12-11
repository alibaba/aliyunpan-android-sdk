package com.alicloud.databox.opensdk

interface AliyunpanScope : AliyunpanCommand {

    fun getHttpMethod(): String

    fun getApi(): String

    fun getRequest(): Map<String, Any?>
}