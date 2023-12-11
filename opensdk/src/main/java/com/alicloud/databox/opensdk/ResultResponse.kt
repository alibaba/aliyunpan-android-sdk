package com.alicloud.databox.opensdk

import org.json.JSONObject

class ResultResponse(val code: Int, val data: Data) {

    override fun toString(): String {
        return "ResultResponse(code=$code, data=$data)"
    }

    class Data(private val rawData: ByteArray) {

        fun getRawBytes() = rawData

        fun asString(): String {
            return String(rawData)
        }

        fun asJSONObject(): JSONObject {
            return JSONObject(asString())
        }

        override fun toString(): String {
            return asString()
        }
    }
}