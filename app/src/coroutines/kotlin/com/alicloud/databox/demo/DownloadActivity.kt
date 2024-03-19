package com.alicloud.databox.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alicloud.databox.demo.ViewHelper.appendWithTime
import com.alicloud.databox.demo.databinding.ActivityTaskBinding
import com.alicloud.databox.demo.databinding.IncludeTaskBinding
import com.alicloud.databox.opensdk.kotlin.AliyunpanClient
import com.alicloud.databox.opensdk.io.BaseTask
import kotlinx.coroutines.launch

class DownloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityTaskBinding.inflate(this.layoutInflater)
        setContentView(binding.root)

        val driveId = intent.getStringExtra(KEY_DRIVE_ID)
        val fileIds = intent.getStringArrayExtra(KEY_FILE_IDS)

        binding.btnStartTask.apply {
            text = "开始下载"
            setOnClickListener {
                startDownloadFileAll(driveId, fileIds, binding.layoutGroup, binding.tvResult)
            }
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
        lifecycleScope.launch {
            val task = try {
                client.buildDownload(driveId, fileId)
            } catch (e: Exception) {
                tvResult.appendWithTime("buildDownload failed: $e")
                return@launch
            }
            tvResult.appendWithTime("buildDownload success")

            val taskBinding = IncludeTaskBinding.inflate(layoutInflater, layoutGroup, true)

            taskBinding.tvTaskName.text = task.getTaskName()
            taskBinding.btnTaskOperate.apply {
                text = "取消"
                setOnClickListener {
                    task.cancel()
                }
            }

            task.addStateChange { state ->
                when (state) {
                    BaseTask.TaskState.Abort -> {
                        taskBinding.tvTaskProcess.text = "取消"
                    }

                    is BaseTask.TaskState.Completed -> {
                        taskBinding.progress.progress = 100
                        taskBinding.tvTaskProcess.text = "完成 下载文件路径=${state.filePath}"
                    }

                    is BaseTask.TaskState.Failed -> {
                        taskBinding.tvTaskProcess.text = "失败 ${state.exception}"
                    }

                    is BaseTask.TaskState.Running -> {
                        val progress = state.getProgress()
                        taskBinding.tvTaskProcess.text =
                            "下载中 ${SizeUtil.getFormatSize(state.completedSize)} / ${SizeUtil.getFormatSize(state.totalSize)}"
                        taskBinding.progress.progress = (progress * 100).toInt()
                    }

                    BaseTask.TaskState.Waiting -> {
                        taskBinding.tvTaskProcess.text = "等待"
                    }
                }
            }
            val startResult = task.start()
            tvResult.appendWithTime("task startResult: $startResult")
        }
    }

    companion object {

        private const val KEY_DRIVE_ID = "key_drive_id"
        private const val KEY_FILE_IDS = "key_file_ids"

        const val MAX_DOWNLOAD_FILE_COUNT = 3

        fun launch(context: Activity, driveId: String, fileIds: Array<String>) {
            context.startActivity(Intent(context, DownloadActivity::class.java).apply {
                putExtra(KEY_DRIVE_ID, driveId)
                putExtra(KEY_FILE_IDS, fileIds)
            })
        }
    }
}