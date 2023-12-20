package com.alicloud.databox.demo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alicloud.databox.demo.ViewHelper.appendWithTime
import com.alicloud.databox.demo.databinding.ActivityMainBinding
import com.alicloud.databox.opensdk.io.BaseTask
import com.alicloud.databox.opensdk.scope.AliyunpanFileScope
import com.alicloud.databox.opensdk.scope.AliyunpanUserScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var defaultDriveId: String? = null
    private var fileId: String? = null

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
            startDownloadFile(defaultDriveId, fileId, tvResult)
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
                        limit = 2,
                        type = "file",
                        orderBy = "size",
                        orderDirection = "ASC"
                    )
                )
                tvResult.appendWithTime("GetFileList success: $response")

                val items = response.data.asJSONObject().optJSONArray("items")
                fileId = items.getJSONObject(0).optString("file_id")
            } catch (e: Exception) {
                tvResult.appendWithTime("GetFileList failed: $e")
            }
        }
    }

    private fun startDownloadFile(defaultDriveId: String?, fileId: String?, tvResult: TextView) {
        lifecycleScope.launch {
            val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return@launch

            val task = try {
                aliyunpanClient.buildDownload(defaultDriveId ?: "", fileId ?: "")
            } catch (e: Exception) {
                tvResult.appendWithTime("buildDownload failed $e")
                return@launch
            }

            // 构建下载任务成功
            tvResult.appendWithTime("buildDownload success $task")

            // 先添加任务状态通知
            task.addStateChange { taskState ->
                when (taskState) {
                    BaseTask.TaskState.Abort -> {
                        tvResult.appendWithTime("taskState Abort ")
                    }

                    is BaseTask.TaskState.Completed -> {
                        tvResult.appendWithTime("taskState Completed ${taskState.filePath}")
                    }

                    is BaseTask.TaskState.Failed -> {
                        tvResult.appendWithTime("taskState Failed ${taskState.exception}")
                    }

                    is BaseTask.TaskState.Running -> {
                        tvResult.appendWithTime("taskState Running ${taskState.completedChunkSize}/${taskState.totalChunkSize} Progress=${taskState.getProgress()}")
                    }

                    BaseTask.TaskState.Waiting -> {
                        tvResult.appendWithTime("taskState Waiting")
                    }
                }
            }

            val startResult = task.start()
            tvResult.appendWithTime("start task = $task startResult = $startResult")
        }
    }
}