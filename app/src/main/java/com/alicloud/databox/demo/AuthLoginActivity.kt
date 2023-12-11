package com.alicloud.databox.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast

class AuthLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_login)

        findViewById<View>(R.id.btnOAuth).setOnClickListener {
            startOAuth()
        }
    }

    private fun startOAuth() {
        val context = this
        AliyunpanApp.aliyunpanClient?.oauth({
            Toast.makeText(context, "开始授权", Toast.LENGTH_SHORT).show()
        }, {
            Toast.makeText(context, "发起授权失败", Toast.LENGTH_SHORT).show()
        })
    }
}