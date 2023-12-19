package com.alicloud.databox.demo

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.alicloud.databox.demo.ViewHelper.appendWithTime
import com.alicloud.databox.opensdk.io.BaseTask
import com.alicloud.databox.opensdk.scope.AliyunpanFileScope
import com.alicloud.databox.opensdk.scope.AliyunpanUserScope

class MainActivity : AppCompatActivity() {

    private var defaultDriveId: String? = null
    private var fileId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvResult = findViewById<TextView>(R.id.tvResult)

        findViewById<View>(R.id.btnOAuth).setOnClickListener {
            startOAuth(tvResult)
        }

        findViewById<View>(R.id.btnOAuthClear).setOnClickListener {
            clearOAuth(tvResult)
        }

        findViewById<View>(R.id.btnUserInfo).setOnClickListener {
            getDriveInfo(tvResult)
        }

        findViewById<View>(R.id.btnFileListInfo).setOnClickListener {
            getFileList(defaultDriveId, tvResult)
        }

        findViewById<View>(R.id.btnFileDownload).setOnClickListener {
            startDownloadFile(defaultDriveId, fileId, tvResult)
        }
    }

    private fun startOAuth(tvResult: TextView) {
        AliyunpanApp.aliyunpanClient?.oauth({
            tvResult.appendWithTime("oauth start")
        }, {
            tvResult.appendWithTime("oauth failed: $it")
        })
    }

    private fun clearOAuth(tvResult: TextView) {
        AliyunpanApp.aliyunpanClient?.clearOauth()
        tvResult.appendWithTime("clear oauth")
    }

    private fun getDriveInfo(tvResult: TextView) {
        AliyunpanApp.aliyunpanClient?.send(AliyunpanUserScope.GetDriveInfo(),
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
        if (driveId.isNullOrEmpty()) {
            return
        }
        AliyunpanApp.aliyunpanClient
            ?.send(
                AliyunpanFileScope.GetFileList(
                    driveId,
                    parentFileId = "root",
                    fields = "*",
                    limit = 2,
                    type = "file",
                    orderBy = "size",
                    orderDirection = "ASC"
                ), {
                    tvResult.appendWithTime("GetFileList success: $it")

                    val items = it.data.asJSONObject().optJSONArray("items")
                    fileId = items.getJSONObject(0).optString("file_id")

                }, {
                    tvResult.appendWithTime("GetFileList failed: $it")
                })
    }

    private fun startDownloadFile(defaultDriveId: String?, fileId: String?, tvResult: TextView) {
        AliyunpanApp.aliyunpanClient?.buildDownload(defaultDriveId ?: "", fileId ?: "", { task ->
            tvResult.appendWithTime("buildDownload success $task")
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
        }, {
            tvResult.appendWithTime("buildDownload failed $it")
        })
    }
}