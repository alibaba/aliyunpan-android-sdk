package com.alicloud.databox.demo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alicloud.databox.demo.databinding.ActivityAuthLoginBinding
import kotlinx.coroutines.launch

class AuthLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAuthLoginBinding.inflate(this.layoutInflater)
        setContentView(binding.root)

        binding.btnOAuth.setOnClickListener {
            startOAuth()
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