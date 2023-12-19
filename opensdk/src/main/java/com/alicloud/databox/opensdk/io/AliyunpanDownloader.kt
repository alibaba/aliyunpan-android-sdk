package com.alicloud.databox.opensdk.io

import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanException
import com.alicloud.databox.opensdk.AliyunpanException.Companion.buildError
import com.alicloud.databox.opensdk.Consumer
import com.alicloud.databox.opensdk.LLogger
import com.alicloud.databox.opensdk.http.OKHttpHelper.download
import com.alicloud.databox.opensdk.scope.AliyunpanFileScope
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil

/**
 * Aliyunpan downloader
 * 下载器
 * 支持分片下载，并通知任务状态和下载分片进度
 * // TODO 支持断点续传 2023/12/19
 * @property client
 * @property downloadFolderPath
 * @constructor Create empty Aliyunpan downloader
 */
internal class AliyunpanDownloader(private val client: AliyunpanClient, private val downloadFolderPath: String) {

    private val handler = client.handler

    /**
     * Download group executor
     * 最大并行下载任务 线程池
     */
    private val downloadGroupExecutor = Executors.newFixedThreadPool(DEFAULT_MAX_DOWNLOAD_TASK_RUNNING_SIZE)

    /**
     * Download sub fork join pool
     * 子任务并发池
     */
    private val downloadSubForkJoinPool = ForkJoinPool(DEFAULT_MAX_DOWNLOAD_CHUNK_RUNNING_SIZE)

    private val downloadRunningTaskMap = ConcurrentHashMap<DownloadTask, Future<*>>()

    init {
        LLogger.log(TAG, "AliyunpanDownloader init")
    }

    fun buildDownload(
        driveId: String,
        fileId: String,
        expireSec: Int? = null,
        onSuccess: Consumer<BaseTask>,
        onFailure: Consumer<Exception>
    ) {

        if (driveId.isEmpty() || fileId.isEmpty()) {
            val exception = AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("driveId or fileId is empty")
            LLogger.log(TAG, "buildDownload failed", exception)
            onFailure.accept(exception)
            return
        }

        for (downloadTask in downloadRunningTaskMap.keys()) {
            if (driveId == downloadTask.driveId && fileId == downloadTask.fileId) {
                LLogger.log(TAG, "buildDownload get running task")
                onSuccess.accept(downloadTask)
                return
            }
        }

        client.send(AliyunpanFileScope.GetFile(driveId, fileId), { fileResponse ->
            val fileJson = fileResponse.data.asJSONObject()
            val fileSize = fileJson.optLong("size")
            val fileName = fileJson.optString("name")
            val fileType = fileJson.optString("type")
            val fileContentHash = fileJson.optString("content_hash")

            if (SUPPORT_FILE_TYPE != fileType) {
                onFailure.accept(AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("no support download type"))
                return@send
            }

            if (fileSize <= 0) {
                onFailure.accept(AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("file size is 0"))
                return@send
            }

            if (fileName.isEmpty()) {
                onFailure.accept(AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("file name is empty"))
                return@send
            }

            LLogger.log(TAG, "buildDownload build task")
            onSuccess.accept(
                DownloadTask(
                    driveId,
                    fileId,
                    expireSec,
                    this,
                    fileContentHash,
                    fileName,
                    fileSize
                )
            )
        }, onFailure)
    }

    private fun download(downloadTask: DownloadTask): Boolean {
        if (downloadRunningTaskMap.containsKey(downloadTask)) {
            return false
        }
        postWaiting(downloadTask)
        downloadRunningTaskMap[downloadTask] = downloadLoop(downloadTask, null, null)
        LLogger.log(TAG, "downloadLoop")
        return true
    }

    private fun downloadLoop(
        downloadTask: DownloadTask,
        lastAllChunkList: List<TaskChunk>?,
        lastDoneChunkSet: Set<TaskChunk>?,
    ): Future<*> {
        return downloadGroupExecutor.submit {

            if (downloadTask.isCancel.get()) {
                postAbort(downloadTask)
                return@submit
            }

            val preDownloadFile = try {
                checkFileExists(downloadTask)
            } catch (e: Exception) {
                postFailed(downloadTask, e)
                return@submit
            }

            if (preDownloadFile != null) {
                postCompleted(downloadTask, preDownloadFile.path)
                return@submit
            }

            val downloadUrl = if (downloadTask.downloadUrlValid()) {
                LLogger.log(TAG, "download url valid")
                downloadTask.getDownloadUrl()
            } else {
                val url = try {
                    fetchDownloadUrl(downloadTask)
                } catch (e: Exception) {
                    postFailed(downloadTask, e)
                    return@submit
                }

                if (url.isEmpty()) {
                    postFailed(
                        downloadTask,
                        AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("download fetch url is empty")
                    )
                    return@submit
                }
                downloadTask.recordDownload(url)
                url
            }

            // 创建下载临时文件
            val downloadTempFile = createDownloadTempFile(downloadTask)
            // 计算分片
            val allChunkList = lastAllChunkList ?: buildChunkList(downloadTask.fileSize)

            // 计算未完成分片
            val unDoneChunkList = if (lastDoneChunkSet.isNullOrEmpty()) {
                allChunkList
            } else {
                allChunkList.filterNot { lastDoneChunkSet.contains(it) }
            }

            postRunning(downloadTask, lastDoneChunkSet?.size ?: 0, allChunkList.size)

            val doneChunkSet = mutableSetOf<TaskChunk>()

            if (lastDoneChunkSet != null) {
                doneChunkSet.addAll(lastDoneChunkSet)
            }

            // 构造本次 下载分片任务集合
            val tasks = unDoneChunkList.map { DownloadRecursiveTask(downloadUrl, it, downloadTempFile) }

            val chunkStep = ceil(allChunkList.size.toFloat() / DEFAULT_MAX_CHUNK_STEP).toInt()

            val chunkedTasks = tasks.chunked(chunkStep)

            for (chunkedTask in chunkedTasks) {
                val invokeAll = downloadSubForkJoinPool.invokeAll(chunkedTask)

                try {
                    for (taskChunkFuture in invokeAll) {
                        val doneChunk = taskChunkFuture.get()
                        doneChunkSet.add(doneChunk)
                    }
                } catch (e: ExecutionException) {
                    postNext(downloadTask, allChunkList, doneChunkSet)
                    return@submit
                }

                if (downloadTask.isCancel.get()) {
                    postAbort(downloadTask)
                    return@submit
                }

                // 完成一组chunk 通知一次进度
                postRunning(downloadTask, doneChunkSet.size, allChunkList.size)
            }

            // 比对分片任务 和完成任务集合
            if (allChunkList.size == doneChunkSet.size) {
                val doneDownloadFile = try {
                    doneDownloadFile(downloadTask, downloadTempFile)
                } catch (e: Exception) {
                    postFailed(downloadTask, e)
                    return@submit
                }
                postCompleted(downloadTask, doneDownloadFile.path)
            } else {
                postNext(downloadTask, allChunkList, doneChunkSet)
            }
        }
    }

    private fun fetchDownloadUrl(downloadTask: DownloadTask): String {
        val resultResponse =
            client.sendSync(
                AliyunpanFileScope.GetFileGetDownloadUrl(
                    downloadTask.driveId,
                    downloadTask.fileId,
                    downloadTask.expireSec
                )
            )
        val downloadJson = resultResponse.data.asJSONObject()
        return downloadJson.optString("url")
    }

    private fun postNext(
        downloadTask: DownloadTask,
        chunkList: List<TaskChunk>,
        doneChunkSet: MutableSet<TaskChunk>
    ) {
        handler.post {
            downloadRunningTaskMap[downloadTask] = downloadLoop(downloadTask, chunkList, doneChunkSet)
            LLogger.log(TAG, "downloadLoop next")
        }
    }

    private fun postRunning(downloadTask: DownloadTask, completedChunkSize: Int, totalChunkSize: Int) {
        handler.post {
            for (consumer in downloadTask.stateChangeList) {
                consumer.accept(BaseTask.TaskState.Running(completedChunkSize, totalChunkSize))
            }
        }
    }

    private fun postWaiting(downloadTask: DownloadTask) {
        handler.post {
            for (consumer in downloadTask.stateChangeList) {
                consumer.accept(BaseTask.TaskState.Waiting)
            }
        }
    }

    private fun postFailed(downloadTask: DownloadTask, e: Exception) {
        handler.post {
            for (consumer in downloadTask.stateChangeList) {
                consumer.accept(BaseTask.TaskState.Failed(e))
            }
            downloadRunningTaskMap.remove(downloadTask)
        }
    }

    private fun postCompleted(downloadTask: DownloadTask, path: String) {
        handler.post {
            for (consumer in downloadTask.stateChangeList) {
                consumer.accept(BaseTask.TaskState.Completed(path))
            }
            downloadRunningTaskMap.remove(downloadTask)
        }
    }

    private fun postAbort(downloadTask: DownloadTask) {
        handler.post {
            for (consumer in downloadTask.stateChangeList) {
                consumer.accept(BaseTask.TaskState.Abort)
            }
            downloadRunningTaskMap.remove(downloadTask)
        }
    }

    private fun cancel(downloadTask: DownloadTask) {
        downloadTask.isCancel.set(true)
    }

    private fun createDownloadTempFile(downloadTask: DownloadTask): File {
        val downloadFolder = File(downloadFolderPath)
        if (!downloadFolder.exists()) {
            downloadFolder.mkdirs()
        }

        val file = File(downloadFolder, downloadTask.fileName + ".download")
        if (!file.exists()) {
            file.createNewFile()
        }

        if (file.exists() && file.isDirectory) {
            file.delete()
        }

        return file
    }

    @Throws(AliyunpanException::class)
    private fun doneDownloadFile(downloadTask: DownloadTask, downloadTempFile: File): File {
        val file = File(downloadFolderPath, downloadTask.fileName)
        if (downloadTempFile.renameTo(file)) {
            return file
        } else {
            throw AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("doneDownloadFile failed")
        }
    }

    @Throws(AliyunpanException::class)
    private fun checkFileExists(downloadTask: DownloadTask): File? {
        val file = File(downloadFolderPath, downloadTask.fileName)

        if (file.exists()) {
            if (file.isFile) {
                return file
            } else {
                throw AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("exit same folder")
            }
        }

        return null
    }

    inner class DownloadRecursiveTask(
        private val url: String,
        private val taskChunk: TaskChunk,
        private val downloadTempFile: File
    ) : Callable<TaskChunk> {

        override fun call(): TaskChunk {
            client.getOkHttpInstance()
                .download(
                    url,
                    taskChunk.chunkStart,
                    taskChunk.chunkStart + taskChunk.chunkSize,
                    downloadTempFile,
                    null
                )
            return taskChunk
        }
    }

    data class TaskChunk(
        val chunkIndex: Int,
        val chunkStart: Long,
        val chunkSize: Long,
    )

    class DownloadTask(
        override val driveId: String,
        override val fileId: String,
        val expireSec: Int? = null,
        private val downloader: AliyunpanDownloader,
        val fileHashPath: String,
        val fileName: String,
        val fileSize: Long
    ) : BaseTask(driveId, fileId) {

        val isCancel = AtomicBoolean(false)

        private var downloadUrl: String? = null
        private var expireSeconds: Long? = null

        internal val stateChangeList = CopyOnWriteArrayList<Consumer<TaskState>>()

        override fun start(): Boolean {
            return downloader.download(this)
        }

        override fun cancel() {
            downloader.cancel(this)
        }

        override fun addStateChange(onChange: Consumer<TaskState>) {
            stateChangeList.add(onChange)
        }

        override fun removeStateChange(onChange: Consumer<TaskState>) {
            stateChangeList.remove(onChange)
        }

        internal fun recordDownload(url: String) {
            downloadUrl = url
            expireSeconds = (System.currentTimeMillis() / 1000) + (expireSec ?: DEFAULT_EXPIRE_SECONDS)
        }

        internal fun getDownloadUrl(): String = downloadUrl ?: ""
        internal fun downloadUrlValid(): Boolean {
            if (downloadUrl.isNullOrEmpty()) {
                return false
            }

            return expireSeconds?.let { (System.currentTimeMillis() / 1000) < it } ?: false
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DownloadTask

            if (driveId != other.driveId) return false
            return fileId == other.fileId
        }

        override fun hashCode(): Int {
            var result = driveId.hashCode()
            result = 31 * result + fileId.hashCode()
            return result
        }

        companion object {

            private const val DEFAULT_EXPIRE_SECONDS = 900
        }
    }

    companion object {

        private const val TAG = "AliyunpanDownloader"

        private const val SUPPORT_FILE_TYPE = "file"

        private const val DEFAULT_MAX_DOWNLOAD_TASK_RUNNING_SIZE = 2

        private const val DEFAULT_MAX_DOWNLOAD_CHUNK_RUNNING_SIZE = 8

        /**
         * Default Max Chunk Size
         * 分片size 最大值
         */
        internal const val DEFAULT_MAX_CHUNK_SIZE = 1024 * 100L

        /**
         * Default Max Chunk Step
         * 最多切100步
         */
        private const val DEFAULT_MAX_CHUNK_STEP = 100

        internal fun buildChunkList(size: Long): List<TaskChunk> {
            if (size <= DEFAULT_MAX_CHUNK_SIZE) {
                return listOf(TaskChunk(0, 0, size))
            }
            val taskChunks = arrayListOf<TaskChunk>()
            val index = (size / DEFAULT_MAX_CHUNK_SIZE).toInt()
            repeat(index) {
                taskChunks.add(TaskChunk(it, it * DEFAULT_MAX_CHUNK_SIZE, DEFAULT_MAX_CHUNK_SIZE))
            }

            val endSize = size % DEFAULT_MAX_CHUNK_SIZE
            if (endSize > 0) {
                taskChunks.add(TaskChunk(index, index * DEFAULT_MAX_CHUNK_SIZE, endSize))
            }

            return taskChunks
        }
    }
}

