package com.alicloud.databox.demo

import android.content.Context
import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanClientConfig

object AliyunpanApp {

    var aliyunpanClient: AliyunpanClient? = null

    fun initApp(context: Context) {
        // 配置
        val config = AliyunpanClientConfig.Builder(context, BuildConfig.APP_KEY)
//            .appSecret("你的appSecret")  // 安全方面 不建议明文Secret使用方式
            .build()
        // 初始化client
        aliyunpanClient = AliyunpanClient.init(config)
    }
}