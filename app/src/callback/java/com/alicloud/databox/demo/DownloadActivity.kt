package com.alicloud.databox.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.alicloud.databox.demo.ViewHelper.appendWithTime
import com.alicloud.databox.demo.databinding.ActivityDownloadBinding
import com.alicloud.databox.demo.databinding.IncludeDownloadTaskBinding
import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.io.BaseTask

class DownloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDownloadBinding.inflate(this.layoutInflater)
        setContentView(binding.root)

        val driveId = intent.getStringExtra(KEY_DRIVE_ID)
        val fileIds = intent.getStringArrayExtra(KEY_FILE_IDS)

        val tvResult = binding.tvResult
        val layoutGroup = binding.layoutGroup

        binding.btnFileDownload.setOnClickListener {
            startDownloadFileAll(driveId, fileIds, layoutGroup, tvResult)
        }
    }

    private fun startDownloadFileAll(
        driveId: String?,
        fileIds: Array<String>?,
        layoutGroup: LinearLayout,
        tvResult: TextView
    ) {
        if (driveId.isNullOrEmpty() || fileIds.isNullOrEmpty()) {
            return
        }

        val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return
        layoutGroup.removeAllViews()
        fileIds.forEach {
            startDownloadFile(aliyunpanClient, driveId, it, layoutGroup, tvResult)
        }
    }

    private fun startDownloadFile(
        client: AliyunpanClient,
        driveId: String,
        fileId: String,
        layoutGroup: LinearLayout,
        tvResult: TextView
    ) {
        client.buildDownload(driveId, fileId, { task ->
            val downloadTaskBinding = IncludeDownloadTaskBinding.inflate(layoutInflater, layoutGroup, true)

            downloadTaskBinding.tvTaskName.text = task.getTaskName()
            downloadTaskBinding.tvTaskProcess.setOnClickListener {
                task.cancel()
            }
            task.addStateChange { state ->
                when (state) {
                    BaseTask.TaskState.Abort -> {
                        downloadTaskBinding.tvTaskProcess.text = "取消"
                    }

                    is BaseTask.TaskState.Completed -> {
                        downloadTaskBinding.progress.progress = 100
                        downloadTaskBinding.tvTaskProcess.text = "完成 ${state.filePath}"
                    }

                    is BaseTask.TaskState.Failed -> {
                        downloadTaskBinding.tvTaskProcess.text = "失败 ${state.exception}"
                    }

                    is BaseTask.TaskState.Running -> {
                        val progress = state.getProgress()
                        downloadTaskBinding.tvTaskProcess.text =
                            "下载中 ${SizeUtil.getFormatSize(state.completedSize)} / ${SizeUtil.getFormatSize(state.totalSize)}"
                        downloadTaskBinding.progress.progress = (progress * 100).toInt()
                    }

                    BaseTask.TaskState.Waiting -> {
                        downloadTaskBinding.tvTaskProcess.text = "等待"
                    }
                }
            }
            val startResult = task.start()
            tvResult.appendWithTime("task startResult: $startResult")
        }, {
            tvResult.appendWithTime("buildDownload failed: $it")
        })
    }

    companion object {

        private const val KEY_DRIVE_ID = "key_drive_id"
        private const val KEY_FILE_IDS = "key_file_ids"

        fun launch(context: Activity, driveId: String, fileIds: Array<String>) {
            context.startActivity(Intent(context, DownloadActivity::class.java).apply {
                putExtra(KEY_DRIVE_ID, driveId)
                putExtra(KEY_FILE_IDS, fileIds)
            })
        }
    }
}