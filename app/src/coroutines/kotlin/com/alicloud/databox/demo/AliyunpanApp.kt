package com.alicloud.databox.demo

import android.content.Context
import com.alicloud.databox.opensdk.AliyunpanClientConfig
import com.alicloud.databox.opensdk.kotlin.AliyunpanClient

object AliyunpanApp {

    var aliyunpanClient: AliyunpanClient? = null

    fun initApp(context: Context) {
        // 配置
        val config = AliyunpanClientConfig.Builder(context, BuildConfig.APP_KEY)
            .build()
        // 初始化client
        aliyunpanClient = AliyunpanClient.init(config)
    }
}