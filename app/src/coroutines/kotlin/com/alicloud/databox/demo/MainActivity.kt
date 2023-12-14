package com.alicloud.databox.demo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alicloud.databox.demo.ViewHelper.appendWithTime
import com.alicloud.databox.demo.databinding.ActivityMainBinding
import com.alicloud.databox.opensdk.scope.AliyunpanFileScope
import com.alicloud.databox.opensdk.scope.AliyunpanUserScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var defaultDriveId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(this.layoutInflater)
        setContentView(binding.root)

        val tvResult = binding.tvResult

        binding.btnOAuth.setOnClickListener {
            startOAuth(tvResult)
        }

        binding.btnOAuthClear.setOnClickListener {
            clearOAuth(tvResult)
        }

        binding.btnUserInfo.setOnClickListener {
            getDriveInfo(tvResult)
        }

        binding.btnFileListInfo.setOnClickListener {
            getFileList(defaultDriveId, tvResult)
        }
    }

    private fun startOAuth(tvResult: TextView) {
        lifecycleScope.launch {
            try {
                AliyunpanApp.aliyunpanClient?.oauth()
                tvResult.appendWithTime("oauth start")
            } catch (e: Exception) {
                tvResult.appendWithTime("oauth failed: $e")
            }
        }
    }

    private fun clearOAuth(tvResult: TextView) {
        AliyunpanApp.aliyunpanClient?.clearOauth()
        tvResult.appendWithTime("clear oauth")
    }

    private fun getDriveInfo(tvResult: TextView) {
        lifecycleScope.launch {
            try {
                val response = AliyunpanApp.aliyunpanClient?.send(AliyunpanUserScope.GetDriveInfo())
                tvResult.appendWithTime("GetDriveInfo success: $response")
                defaultDriveId = response?.data?.asJSONObject()?.optString("default_drive_id")
            } catch (e: Exception) {
                tvResult.appendWithTime("GetDriveInfo failed: $e")
            }
        }
    }

    private fun getFileList(driveId: String?, tvResult: TextView) {
        if (driveId.isNullOrEmpty()) {
            return
        }

        lifecycleScope.launch {
            try {
                val response = AliyunpanApp.aliyunpanClient?.send(
                    AliyunpanFileScope.GetFileList(
                        driveId,
                        parentFileId = "root",
                        limit = 2
                    )
                )
                tvResult.appendWithTime("GetFileList success: $response")
            } catch (e: Exception) {
                tvResult.appendWithTime("GetFileList failed: $e")
            }
        }
    }
}