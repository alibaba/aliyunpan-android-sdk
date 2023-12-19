package com.alicloud.databox.demo

import android.content.Context
import android.os.Environment
import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanClientConfig
import java.io.File

object AliyunpanApp {

    var aliyunpanClient: AliyunpanClient? = null

    fun initApp(context: Context) {
        // 配置
        val config = AliyunpanClientConfig.Builder(context, BuildConfig.APP_KEY)
            .downFolder(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Aliyunpan-Sdk"
                )
            )
            .build()
        // 初始化client
        aliyunpanClient = AliyunpanClient.init(config)
    }
}