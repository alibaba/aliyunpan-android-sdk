package com.alicloud.databox.opensdk.http

import com.alicloud.databox.opensdk.AliyunpanException

class AliyunpanHttpException(override val code: String, override val message: String) :
    AliyunpanException(code, message) {

    override fun toString(): String {
        return "AliyunpanHttpException(code='$code', message='$message')"
    }
}