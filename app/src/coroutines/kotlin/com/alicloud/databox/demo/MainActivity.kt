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
    private var fileIdList: List<String>? = null

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

        binding.btnFileDownload.setOnClickListener {
            startDownloadFile(defaultDriveId, fileIdList, tvResult)
        }

        binding.btnFileUpload.setOnClickListener {
            startUploadFile(defaultDriveId, tvResult)
        }
    }

    private fun startOAuth(tvResult: TextView) {
        lifecycleScope.launch {
            val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return@launch
            Result.runCatching {
                aliyunpanClient.oauth()
            }.onSuccess {
                tvResult.appendWithTime("oauth start")
            }.onFailure {
                tvResult.appendWithTime("oauth failed: $it")
            }
        }
    }

    private fun clearOAuth(tvResult: TextView) {
        val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return
        aliyunpanClient.clearOauth()
        tvResult.appendWithTime("clear oauth")
    }

    private fun getDriveInfo(tvResult: TextView) {
        lifecycleScope.launch {
            val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return@launch
            try {
                val response = aliyunpanClient.send(AliyunpanUserScope.GetDriveInfo())
                tvResult.appendWithTime("GetDriveInfo success: $response")
                defaultDriveId = response.data.asJSONObject().optString("default_drive_id")
            } catch (e: Exception) {
                tvResult.appendWithTime("GetDriveInfo failed: $e")
            }
        }
    }

    private fun getFileList(driveId: String?, tvResult: TextView) {
        lifecycleScope.launch {
            val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return@launch
            try {
                val response = aliyunpanClient.send(
                    AliyunpanFileScope.GetFileList(
                        driveId ?: "",
                        parentFileId = "root",
                        fields = "*",
                        limit = DownloadActivity.MAX_DOWNLOAD_FILE_COUNT,
                        type = "file",
                        orderBy = "size",
                        orderDirection = "DESC"
                    )
                )
                tvResult.appendWithTime("GetFileList success: $response")

                val items = response.data.asJSONObject().optJSONArray("items")
                val fileNameList = arrayListOf<String>()

                val fileIds = arrayListOf<String>()
                for (i in 0 until items.length()) {
                    val itemJsonObject = items.optJSONObject(i)
                    fileIds.add(itemJsonObject.optString("file_id"))
                    fileNameList.add(itemJsonObject.optString("name"))
                }
                tvResult.appendWithTime("GetFileList success items size = ${items.length()} fileNameList = $fileNameList")

                fileIdList = fileIds
            } catch (e: Exception) {
                tvResult.appendWithTime("GetFileList failed: $e")
            }
        }
    }

    private fun startDownloadFile(defaultDriveId: String?, fileIdList: List<String>?, tvResult: TextView) {
        if (defaultDriveId.isNullOrEmpty() || fileIdList.isNullOrEmpty()) {
            tvResult.appendWithTime("startDownloadFile defaultDriveId/fileIdList is null or empty")
            return
        }
        DownloadActivity.launch(this, defaultDriveId, fileIdList.toTypedArray())
    }

    private fun startUploadFile(defaultDriveId: String?, tvResult: TextView) {
        if (defaultDriveId.isNullOrEmpty()) {
            tvResult.appendWithTime("startUploadFile defaultDriveId is null or empty")
            return
        }

        UploadActivity.launch(this, defaultDriveId)
    }
}