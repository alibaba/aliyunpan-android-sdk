package com.alicloud.databox.opensdk

import android.app.Activity
import okhttp3.OkHttpClient

internal interface AliyunpanBaseClient {

    fun clearOauth()

    fun fetchToken(activity: Activity)

    fun isInstanceYunpanApp(): Boolean

    fun getOkHttpInstance(): OkHttpClient
}