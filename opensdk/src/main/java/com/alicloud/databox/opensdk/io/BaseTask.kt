package com.alicloud.databox.opensdk.io

import com.alicloud.databox.opensdk.Consumer

abstract class BaseTask(
    internal open val driveId: String,
    internal open val fileId: String,
) {

    abstract fun getTaskName(): String

    /**
     * Start
     *
     * @return 正在运行中返回false 否则返回true
     */
    abstract fun start(): Boolean

    /**
     * Cancel
     * 取消任务
     */
    abstract fun cancel()

    abstract fun addStateChange(onChange: Consumer<TaskState>)

    abstract fun removeStateChange(onChange: Consumer<TaskState>)

    fun getDriveId(): String = driveId
    fun getFileId(): String = fileId

    /**
     * Task state
     * 密封类封装各运行状态 和关联数据
     * @constructor Create empty Task state
     */
    sealed class TaskState {

        /**
         * 等待
         */
        object Waiting : TaskState()

        /**
         * 正在运行
         */
        class Running(val completedSize: Long, val totalSize: Long) : TaskState() {
            /**
             * Get progress
             *
             * @return 返回值范围 带小数点 0.0..1.0
             */
            fun getProgress(): Float {
                return completedSize.toFloat() / totalSize
            }
        }

//        /**
//         * 暂停
//         */
//        object Paused : TaskState()

        /**
         * 已完成
         */
        class Completed(val filePath: String) : TaskState()

        /**
         * 失败
         */
        class Failed(val exception: Exception) : TaskState()

        /**
         * 取消
         */
        object Abort : TaskState()
    }
}