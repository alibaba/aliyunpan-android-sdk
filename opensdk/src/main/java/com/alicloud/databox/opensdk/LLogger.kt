package com.alicloud.databox.opensdk

import android.util.Log

internal object LLogger {

    private const val TAG = "Aliyunpan"

    fun log(tag: String, msg: String) {
        Log.d(TAG, "$tag $msg")
    }

    fun log(tag: String, msg: String, exception: Exception) {
        Log.e(TAG, "$tag $msg", exception)
    }
}