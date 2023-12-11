package com.alicloud.databox.demo

import android.content.Context
import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanClientConfig

object AliyunpanApp {

    var aliyunpanClient: AliyunpanClient? = null

    fun initApp(context: Context) {
        val config = AliyunpanClientConfig.Builder(context, BuildConfig.APP_KEY)
            .build()
        aliyunpanClient = AliyunpanClient.init(config)
    }
}