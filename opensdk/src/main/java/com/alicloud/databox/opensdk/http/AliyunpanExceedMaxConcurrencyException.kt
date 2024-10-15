package com.alicloud.databox.opensdk.http

import com.alicloud.databox.opensdk.AliyunpanException

class AliyunpanExceedMaxConcurrencyException(override val code: String, override val message: String) :
    AliyunpanException(code, message) {

    override fun toString(): String {
        return "AliyunpanExceedMaxConcurrencyException(code='$code', message='$message')"
    }

}