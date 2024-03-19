package com.alicloud.databox.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.alicloud.databox.demo.ViewHelper.appendWithTime
import com.alicloud.databox.demo.databinding.ActivityTaskBinding
import com.alicloud.databox.demo.databinding.IncludeTaskBinding
import com.alicloud.databox.opensdk.io.BaseTask

class UploadActivity : AppCompatActivity() {

    private val binding: ActivityTaskBinding by lazy { ActivityTaskBinding.inflate(this.layoutInflater) }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            val filePath = PathUtils.getPathFromUri(this, it)
            startUploadFile(filePath)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnStartTask.apply {
            text = "开始上传"
            setOnClickListener {
                pickFile()
            }
        }
    }

    private fun pickFile() {
        getContent.launch("*/*")
    }

    private fun startUploadFile(filePath: String?) {

        val driveId = intent.getStringExtra(KEY_DRIVE_ID)

        val layoutGroup = binding.layoutGroup
        val tvResult = binding.tvResult

        if (driveId.isNullOrEmpty()) {
            tvResult.appendWithTime("driveId is null or empty")
            return
        }

        if (filePath.isNullOrEmpty()) {
            tvResult.appendWithTime("path is null or empty")
            return
        }

        tvResult.appendWithTime("path = $filePath")
        val aliyunpanClient = AliyunpanApp.aliyunpanClient ?: return
        aliyunpanClient.buildUpload(driveId, filePath, { task ->
            tvResult.appendWithTime("buildUpload success")

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
                        taskBinding.tvTaskProcess.text = "完成 上传文件路径=${state.filePath}"
                    }

                    is BaseTask.TaskState.Failed -> {
                        taskBinding.tvTaskProcess.text = "失败 ${state.exception}"
                    }

                    is BaseTask.TaskState.Running -> {
                        val progress = state.getProgress()
                        taskBinding.tvTaskProcess.text =
                            "上传中 ${SizeUtil.getFormatSize(state.completedSize)} / ${SizeUtil.getFormatSize(state.totalSize)}"
                        taskBinding.progress.progress = (progress * 100).toInt()
                    }

                    BaseTask.TaskState.Waiting -> {
                        taskBinding.tvTaskProcess.text = "等待"
                    }
                }
            }

            val startResult = task.start()
            tvResult.appendWithTime("task startResult: $startResult")
        }, {
            tvResult.appendWithTime("buildUpload failed $it")
        })
    }

    companion object {

        private const val KEY_DRIVE_ID = "key_drive_id"

        fun launch(context: Activity, driveId: String) {
            context.startActivity(Intent(context, UploadActivity::class.java).apply {
                putExtra(KEY_DRIVE_ID, driveId)
            })
        }
    }
}