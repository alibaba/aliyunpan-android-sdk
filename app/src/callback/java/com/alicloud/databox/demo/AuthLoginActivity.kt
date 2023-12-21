package com.alicloud.databox.demo

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alicloud.databox.opensdk.auth.AliyunpanAuthorizeQRCodeStatus
import com.bumptech.glide.Glide

class AuthLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_login)

        findViewById<View>(R.id.btnOAuth).setOnClickListener {
            startOAuth()
        }

        findViewById<View>(R.id.btnOAuthQRCode).setOnClickListener {
            startOAuthQRCode(findViewById<ImageView>(R.id.ivQRCode))
        }
    }

    private fun startOAuthQRCode(ivQRCode: ImageView) {
        val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return
        val context = this
        aliyunpanClient.oauthQRCode({ task ->
            Toast.makeText(context, "开始扫码授权", Toast.LENGTH_SHORT).show()
            val qrCodeUrl = task.getQRCodeUrl()
            Glide.with(context)
                .load(qrCodeUrl)
                .into(ivQRCode)

            task.addStateChange {
                when (it) {
                    AliyunpanAuthorizeQRCodeStatus.WAIT_LOGIN -> {
                        Toast.makeText(context, "等待扫码", Toast.LENGTH_SHORT).show()
                    }

                    AliyunpanAuthorizeQRCodeStatus.SCAN_SUCCESS -> {
                        Toast.makeText(context, "扫码成功", Toast.LENGTH_SHORT).show()
                    }

                    AliyunpanAuthorizeQRCodeStatus.LOGIN_SUCCESS -> {
                        Toast.makeText(context, "授权成功", Toast.LENGTH_SHORT).show()
                    }

                    AliyunpanAuthorizeQRCodeStatus.QRCODE_EXPIRED -> {
                        Toast.makeText(context, "二维码过期", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }, {
            Toast.makeText(context, "发起扫码授权失败", Toast.LENGTH_SHORT).show()
        })
    }

    private fun startOAuth() {
        val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return
        val context = this
        aliyunpanClient.oauth({
            Toast.makeText(context, "开始授权", Toast.LENGTH_SHORT).show()
        }, {
            Toast.makeText(context, "发起授权失败", Toast.LENGTH_SHORT).show()
        })
    }
}