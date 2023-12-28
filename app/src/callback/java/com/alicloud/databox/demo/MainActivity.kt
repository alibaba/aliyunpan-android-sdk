package com.alicloud.databox.demo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.alicloud.databox.demo.ViewHelper.appendWithTime
import com.alicloud.databox.demo.databinding.ActivityMainBinding
import com.alicloud.databox.opensdk.scope.AliyunpanFileScope
import com.alicloud.databox.opensdk.scope.AliyunpanUserScope

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
    }

    private fun startOAuth(tvResult: TextView) {
        val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return
        aliyunpanClient.oauth({
            tvResult.appendWithTime("oauth start")
        }, {
            tvResult.appendWithTime("oauth failed: $it")
        })
    }

    private fun clearOAuth(tvResult: TextView) {
        val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return
        aliyunpanClient.clearOauth()
        tvResult.appendWithTime("clear oauth")
    }

    private fun getDriveInfo(tvResult: TextView) {
        val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return
        aliyunpanClient.send(AliyunpanUserScope.GetDriveInfo(),
            { result ->
                // 成功结果
                tvResult.appendWithTime("GetDriveInfo success: $result")

                defaultDriveId = result.data.asJSONObject().optString("default_drive_id")
            }, {
                // 失败结果
                tvResult.appendWithTime("GetDriveInfo failed: $it")
            })
    }

    private fun getFileList(driveId: String?, tvResult: TextView) {
        val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return

        aliyunpanClient.send(AliyunpanFileScope.GetFileList(
            driveId ?: "",
            parentFileId = "root",
            fields = "*",
            limit = 3,
            type = "file",
            orderBy = "size",
            orderDirection = "DESC"
        ), {
            tvResult.appendWithTime("GetFileList success")

            val items = it.data.asJSONObject().optJSONArray("items")
            val fileNameList = arrayListOf<String>()

            val fileIds = arrayListOf<String>()
            for (i in 0 until items.length()) {
                val itemJsonObject = items.optJSONObject(i)
                fileIds.add(itemJsonObject.optString("file_id"))
                fileNameList.add(itemJsonObject.optString("name"))
            }
            tvResult.appendWithTime("GetFileList success items size = ${items.length()} fileNameList = $fileNameList")

            fileIdList = fileIds

        }, {
            tvResult.appendWithTime("GetFileList failed: $it")
        })
    }

    private fun startDownloadFile(defaultDriveId: String?, fileIdList: List<String>?, tvResult: TextView) {
        if (defaultDriveId.isNullOrEmpty() || fileIdList.isNullOrEmpty()) {
            tvResult.appendWithTime("startDownloadFile defaultDriveId/fileIdList is null or empty")
            return
        }
        DownloadActivity.launch(this, defaultDriveId, fileIdList.toTypedArray())
    }
}