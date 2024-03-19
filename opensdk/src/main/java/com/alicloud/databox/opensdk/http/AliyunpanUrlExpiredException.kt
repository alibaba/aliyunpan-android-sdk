package com.alicloud.databox.opensdk.http

import com.alicloud.databox.opensdk.AliyunpanException

class AliyunpanUrlExpiredException(override val code: String, override val message: String) :
    AliyunpanException(code, message) {

    override fun toString(): String {
        return "AliyunpanUrlExpiredException(code='$code', message='$message')"
    }

}