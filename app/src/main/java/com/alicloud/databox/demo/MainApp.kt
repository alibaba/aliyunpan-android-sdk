package com.alicloud.databox.demo

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.alicloud.databox.opensdk.AliyunpanAction
import com.alicloud.databox.opensdk.AliyunpanBroadcastHelper

class MainApp : Application() {

    private val receive: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            val message = intent.getStringExtra("message")

            when (AliyunpanAction.valueOf(action)) {
                //登录成功
                AliyunpanAction.NOTIFY_LOGIN_SUCCESS -> {
                    Toast.makeText(context, "授权成功", Toast.LENGTH_SHORT).show()
                    // 授权成功 示例打开主页
                    context.startActivity(
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }

                //登出
                AliyunpanAction.NOTIFY_LOGOUT -> {
                    Toast.makeText(context, "登录状态失效", Toast.LENGTH_SHORT).show()
                }

                //登录取消
                AliyunpanAction.NOTIFY_LOGIN_CANCEL -> {
                    Toast.makeText(context, "授权取消 message=$message", Toast.LENGTH_SHORT).show()
                }

                //登录失败
                AliyunpanAction.NOTIFY_LOGIN_FAILED -> {
                    Toast.makeText(context, "授权失败 message=$message", Toast.LENGTH_SHORT).show()
                }

                //登录状态重置
                AliyunpanAction.NOTIFY_RESET_STATUS -> {
                    Toast.makeText(context, "授权状态重置", Toast.LENGTH_SHORT).show()
                    // 授权状态重置 示例打开授权页
                    context.startActivity(
                        Intent(context, AuthLoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }

                else -> {
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 注册广播
        AliyunpanBroadcastHelper.registerReceiver(this, receive)
        // 开始初始化
        AliyunpanApp.initApp(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        AliyunpanBroadcastHelper.unregisterReceiver(this, receive)
    }
}