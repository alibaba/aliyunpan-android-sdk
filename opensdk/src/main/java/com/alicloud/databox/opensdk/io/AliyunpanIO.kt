package com.alicloud.databox.opensdk.io

import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.Consumer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

internal abstract class AliyunpanIO<T : BaseTask>(protected val client: AliyunpanClient) {

    protected val handler = client.handler

    protected val runningTaskMap = ConcurrentHashMap<T, Future<*>>()

    open fun buildThreadGroupExecutor(maximumPoolSize: Int, threadNamePrefix: String): ThreadPoolExecutor {
        return ThreadPoolExecutor(
            maximumPoolSize,
            maximumPoolSize,
            0,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            object : ThreadFactory {

                private val group = Thread.currentThread().threadGroup

                private val poolNumber = AtomicInteger(0)

                override fun newThread(r: Runnable?): Thread {
                    return Thread(group, r, "$threadNamePrefix-${poolNumber.getAndIncrement()}", 0)
                }
            })
    }

    protected fun postSuccess(onSuccess: Consumer<BaseTask>, task: T) {
        handler.post {
            onSuccess.accept(task)
        }
    }

    protected fun postFailure(onFailure: Consumer<Exception>, t: Exception) {
        handler.post {
            onFailure.accept(t)
        }
    }

    protected fun postRunning(task: BaseTask, completedSize: Long, totalSize: Long) {
        handler.post {
            for (consumer in task.stateChangeList) {
                consumer.accept(BaseTask.TaskState.Running(completedSize, totalSize))
            }
        }
    }

    protected fun postWaiting(task: BaseTask) {
        handler.post {
            for (consumer in task.stateChangeList) {
                consumer.accept(BaseTask.TaskState.Waiting)
            }
        }
    }

    protected fun postFailed(task: BaseTask, e: Exception) {
        handler.post {
            for (consumer in task.stateChangeList) {
                consumer.accept(BaseTask.TaskState.Failed(e))
            }
            runningTaskMap.remove(task)
        }
    }

    protected fun postCompleted(task: BaseTask, path: String) {
        handler.post {
            for (consumer in task.stateChangeList) {
                consumer.accept(BaseTask.TaskState.Completed(path))
            }
            runningTaskMap.remove(task)
        }
    }

    protected fun postAbort(task: BaseTask) {
        handler.post {
            for (consumer in task.stateChangeList) {
                consumer.accept(BaseTask.TaskState.Abort)
            }
            runningTaskMap.remove(task)
        }
    }

    data class TaskChunk(
        val chunkIndex: Int,
        val chunkStart: Long,
        val chunkSize: Long,
    )

    companion object {

        internal const val SUPPORT_FILE_TYPE = "file"

        /**
         * Max Chunk Count
         * 最大 分片数
         */
        internal const val MAX_CHUNK_COUNT = 10000

        /**
         * Default Max Chunk Size
         * 最大 分片size
         */
        internal const val MAX_CHUNK_SIZE = 2 * 1024 * 1024L

        internal fun buildChunkList(size: Long): List<TaskChunk> {
            if (size <= MAX_CHUNK_SIZE) {
                return listOf(TaskChunk(0, 0, size))
            }
            val taskChunks = arrayListOf<TaskChunk>()

            val fixedChunkSize = max(MAX_CHUNK_SIZE, size / MAX_CHUNK_COUNT)
            val index = (size / fixedChunkSize).toInt()
            val remainChunkSize = size % fixedChunkSize
            for (i in 0 until index) {
                val isLast = i == (index - 1)
                val chunkSize = if (isLast) fixedChunkSize + remainChunkSize else fixedChunkSize
                taskChunks.add(TaskChunk(i, i * fixedChunkSize, chunkSize))
            }
            return taskChunks
        }
    }
}