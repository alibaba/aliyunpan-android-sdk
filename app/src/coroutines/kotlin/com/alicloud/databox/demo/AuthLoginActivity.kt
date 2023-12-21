package com.alicloud.databox.demo

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alicloud.databox.demo.databinding.ActivityAuthLoginBinding
import com.alicloud.databox.opensdk.auth.AliyunpanAuthorizeQRCodeStatus
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class AuthLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAuthLoginBinding.inflate(this.layoutInflater)
        setContentView(binding.root)

        binding.btnOAuth.setOnClickListener {
            startOAuth()
        }

        binding.btnOAuthQRCode.setOnClickListener {
            startOAuthQRCode(binding.ivQRCode)
        }
    }

    private fun startOAuthQRCode(ivQRCode: ImageView) {
        val context = this
        lifecycleScope.launch{
            val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return@launch
            try {
                val task = aliyunpanClient.oauthQRCode()
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
            } catch (e: Exception) {
                Toast.makeText(context, "发起扫码授权失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startOAuth() {
        val context = this
        lifecycleScope.launch {
            try {
                val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return@launch
                aliyunpanClient.oauth()
                Toast.makeText(context, "开始授权", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "发起授权失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}