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
        // 提示 因为appId绑定申请时的包名，当前demo项目在您本地无法成功授权 demo代码只做参考 需要在appId对应项目使用以下代码
        val config = AliyunpanClientConfig.Builder(context, BuildConfig.APP_KEY)
            // 云盘上传文件 需要写权限
            .appendScope(AliyunpanClientConfig.SCOPE_FILE_WRITE)
            .downFolder(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Aliyunpan"
                )
            )
            .build()
        // 初始化client
        aliyunpanClient = AliyunpanClient.init(config)
    }
}