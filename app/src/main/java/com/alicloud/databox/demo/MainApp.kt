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

            when (AliyunpanAction.valueOf(action)) {
                AliyunpanAction.NOTIFY_LOGIN_SUCCESS -> {
                    Toast.makeText(context, "授权成功", Toast.LENGTH_SHORT).show()
                    context.startActivity(
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }

                AliyunpanAction.NOTIFY_LOGOUT,
                AliyunpanAction.NOTIFY_LOGIN_CANCEL,
                AliyunpanAction.NOTIFY_LOGIN_FAILED -> {
                    Toast.makeText(context, "授权失败", Toast.LENGTH_SHORT).show()
                }

                AliyunpanAction.NOTIFY_RESET_STATUS -> {
                    Toast.makeText(context, "授权状态重置 跳转授权页", Toast.LENGTH_SHORT).show()
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
        AliyunpanBroadcastHelper.registerReceiver(this, receive)
        AliyunpanApp.initApp(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        AliyunpanBroadcastHelper.unregisterReceiver(this, receive)
    }
}