package com.alicloud.databox.opensdk.io

import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanClientConfig
import com.alicloud.databox.opensdk.AliyunpanException
import com.alicloud.databox.opensdk.AliyunpanException.Companion.buildError
import com.alicloud.databox.opensdk.Consumer
import com.alicloud.databox.opensdk.LLogger
import com.alicloud.databox.opensdk.http.AliyunpanExceedMaxConcurrencyException
import com.alicloud.databox.opensdk.http.AliyunpanUrlExpiredException
import com.alicloud.databox.opensdk.http.OKHttpHelper.download
import com.alicloud.databox.opensdk.scope.AliyunpanFileScope
import java.io.File
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import kotlin.math.min

/**
 * Aliyunpan downloader
 * 下载器
 * 支持分片下载，并通知任务状态和进度
 * // TODO 支持断点续传 2023/12/19
 * @property client
 * @property downloadFolderPath
 * @constructor Create empty Aliyunpan downloader
 */
internal class AliyunpanDownloader(
    client: AliyunpanClient,
    private val downloadFolderPath: String,
    private val appLevel: AliyunpanClientConfig.AppLevel,
) :
    AliyunpanIO<AliyunpanDownloader.DownloadTask>(client) {

    /**
     * Download group executor
     * 最大并行下载任务 线程池
     */
    private val downloadGroupExecutor =
        buildThreadGroupExecutor(DEFAULT_DOWNLOAD_TASK_SIZE, "download-group")

    private val downloadGroupTaskExecutor =
        buildThreadGroupExecutor(appLevel.downloadTaskLimit, "download-task")

    init {
        LLogger.log(TAG, "AliyunpanDownloader init")
    }

    fun buildDownload(
        driveId: String,
        fileId: String,
        expireSec: Int?,
        onSuccess: Consumer<BaseTask>,
        onFailure: Consumer<Exception>
    ) {

        val wrapOnFailure = Consumer<Exception> { t ->
            LLogger.log(TAG, "buildDownload failed", t)
            postFailure(onFailure, t)
        }

        if (driveId.isEmpty() || fileId.isEmpty()) {
            wrapOnFailure.accept(AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("driveId or fileId is empty"))
            return
        }

        // 运行中去重
        for (task in runningTaskMap.keys()) {
            if (driveId == task.driveId && fileId == task.fileId) {
                LLogger.log(TAG, "buildDownload get running task")
                postSuccess(onSuccess, task)
                return
            }
        }

        client.send(
            AliyunpanFileScope.GetFile(driveId, fileId), {
                val fileJson = it.data.asJSONObject()
                val fileParentFileId = fileJson.optString("parent_file_id")
                val fileSize = fileJson.optLong("size")
                val fileName = fileJson.optString("name")
                val fileType = fileJson.optString("type")
                val fileContentHash = fileJson.optString("content_hash")

                if (SUPPORT_FILE_TYPE != fileType) {
                    wrapOnFailure.accept(AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("no support download type"))
                    return@send
                }

                if (fileSize <= 0) {
                    wrapOnFailure.accept(AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("file size is 0"))
                    return@send
                }

                if (fileName.isEmpty()) {
                    wrapOnFailure.accept(AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("file name is empty"))
                    return@send
                }

                val validExpireSec = expireSec ?: appLevel.downloadUrlExpireSec

                LLogger.log(TAG, "buildDownload success")
                val downloadTask = DownloadTask(
                    this,
                    driveId,
                    fileId,
                    fileParentFileId,
                    validExpireSec,
                    fileContentHash,
                    fileName,
                    fileSize
                )
                postSuccess(onSuccess, downloadTask)
            }, wrapOnFailure
        )
    }

    private fun download(downloadTask: DownloadTask): Boolean {
        if (runningTaskMap.containsKey(downloadTask)) {
            return false
        }
        postWaiting(downloadTask)
        runningTaskMap[downloadTask] = downloadLoop(downloadTask, null, null)
        LLogger.log(TAG, "downloadLoop")
        return true
    }

    private fun downloadLoop(
        downloadTask: DownloadTask,
        lastAllChunkList: List<TaskChunk>?,
        lastDoneChunkSet: Set<TaskChunk>?,
    ): Future<*> {
        return downloadGroupExecutor.submit {

            if (checkCancel(downloadTask)) return@submit

            // 前置检查 是否有已经下载文件
            try {
                val hasLocalFile = checkFileExists(downloadTask)
                if (hasLocalFile != null) {
                    postCompleted(downloadTask, hasLocalFile.path)
                    return@submit
                }
            } catch (e: Exception) {
                postFailed(downloadTask, e)
                return@submit
            }

            val downloadUrl = downloadTask.getValidDownloadUrl() ?: try {
                val downloadUrl = fetchDownloadUrl(downloadTask)
                downloadTask.recordDownloadUrl(downloadUrl)
                LLogger.log(TAG, "fetchDownloadUrl success")
                downloadUrl
            } catch (e: Exception) {
                LLogger.log(TAG, "fetchDownloadUrl failed", e)
                postFailed(downloadTask, e)
                return@submit
            }

            if (downloadUrl.isEmpty()) {
                LLogger.log(TAG, "download url is empty")
                postFailed(
                    downloadTask,
                    AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("download url is empty")
                )
                return@submit
            }

            // 创建下载临时文件
            val downloadTempFile = createDownloadTempFile(downloadTask)
            // 计算分片
            val allChunkList = lastAllChunkList ?: buildChunkList(downloadTask.fileSize, MAX_CHUNK_COUNT)

            val doneChunkSet = mutableSetOf<TaskChunk>()

            if (lastDoneChunkSet != null) {
                doneChunkSet.addAll(lastDoneChunkSet)
            }

            postRunning(downloadTask, sumOfCompletedSize(doneChunkSet), downloadTask.fileSize)

            // 计算未完成分片
            var unDoneChunkList = if (doneChunkSet.isEmpty()) {
                allChunkList.toMutableList()
            } else {
                allChunkList.filterNot { doneChunkSet.contains(it) }
            }

            try {
                var retryTimes = 6
                while (unDoneChunkList.isNotEmpty() && retryTimes > 0) {
                    runDownloadTask(downloadTask, downloadTempFile, doneChunkSet, unDoneChunkList)

                    unDoneChunkList = unDoneChunkList.filterNot { doneChunkSet.contains(it) }
                    retryTimes--
                }
            } catch (e: Exception) {
                if (e is ExecutionException) {
                    when (val throwable = e.cause) {
                        is InterruptedException -> {
                            // 取消
                            postAbort(downloadTask)
                            return@submit
                        }

                        is AliyunpanUrlExpiredException -> {
                            // 过期重试
                            postNext(downloadTask, allChunkList, doneChunkSet)
                            return@submit
                        }

                        is IOException -> {
                            // io异常
                            postFailed(downloadTask, throwable)
                            return@submit
                        }

                        else -> {
                            postFailed(downloadTask, e)
                            return@submit
                        }
                    }
                } else {
                    postFailed(downloadTask, e)
                    return@submit
                }
            }

            // 比对分片任务和完成任务集合
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

    private fun runDownloadTask(
        downloadTask: DownloadTask,
        downloadTempFile: File,
        doneChunkSet: MutableSet<TaskChunk>,
        unDoneChunkList: List<TaskChunk>
    ) {
        // 构造本次 下载分片任务集合
        val tasks = unDoneChunkList.map { DownloadRecursiveTask(downloadTask, it, downloadTempFile) }

        var taskChunkSize = 1
        val taskIterator = tasks.iterator()
        while (taskIterator.hasNext()) {
            val taskChunk = ArrayList<Callable<TaskChunk>>(taskChunkSize)
            repeat(taskChunkSize) {
                if (taskIterator.hasNext()) {
                    taskChunk.add(taskIterator.next())
                }
            }

            val futures = taskChunk.map { downloadGroupTaskExecutor.submit(it) }

            for (taskChunkFuture in futures) {
                val doneChunk = try {
                    taskChunkFuture.get()
                } catch (e: ExecutionException) {
                    if (e.cause is AliyunpanExceedMaxConcurrencyException) {
                        null
                    } else {
                        throw e
                    }
                }

                if (checkCancel(downloadTask)) throw ExecutionException(InterruptedException("download is cancel"))

                if (doneChunk != null) {
                    doneChunkSet.add(doneChunk)
                    postRunning(downloadTask, sumOfCompletedSize(doneChunkSet), downloadTask.fileSize)
                }
            }

            if (checkCancel(downloadTask)) throw ExecutionException(InterruptedException("download is cancel"))

            // 队列空 递增任务分片数
            if (downloadGroupTaskExecutor.queue.isEmpty()) {
                taskChunkSize = min(downloadGroupTaskExecutor.corePoolSize, taskChunkSize + 1)
            }
        }
    }

    private fun sumOfCompletedSize(doneChunkSet: MutableSet<TaskChunk>) =
        if (doneChunkSet.isEmpty()) 0 else doneChunkSet.sumOf { it.chunkSize }

    private fun checkCancel(downloadTask: DownloadTask): Boolean {
        if (downloadTask.isCancel()) {
            postAbort(downloadTask)
            return true
        }
        return false
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
        return resultResponse.data.asJSONObject().optString("url")
    }

    private fun postNext(
        downloadTask: DownloadTask,
        allChunkList: List<TaskChunk>,
        doneChunkSet: MutableSet<TaskChunk>
    ) {
        handler.post {
            runningTaskMap[downloadTask] = downloadLoop(downloadTask, allChunkList, doneChunkSet)
            LLogger.log(TAG, "downloadLoop next")
        }
    }

    private fun createDownloadTempFile(downloadTask: DownloadTask): File {
        val downloadFolder = File(downloadFolderPath)
        if (!downloadFolder.exists()) {
            downloadFolder.mkdirs()
        }

        val downloadTempFile = File(downloadFolder, downloadTask.fileName + ".download")
        if (!downloadTempFile.exists()) {
            downloadTempFile.createNewFile()
        }

        return downloadTempFile
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
        private val downloadTask: DownloadTask,
        private val taskChunk: TaskChunk,
        private val downloadTempFile: File,
    ) : Callable<TaskChunk> {

        override fun call(): TaskChunk {

            val downloadValidUrl = downloadTask.getValidDownloadUrl()

            if (downloadValidUrl.isNullOrEmpty()) {
                throw AliyunpanUrlExpiredException(
                    AliyunpanException.CODE_DOWNLOAD_ERROR,
                    "download valid url is null or empty"
                )
            }

            if (downloadTask.isCancel()) {
                throw InterruptedException("download is cancel")
            }

            LLogger.log(TAG, "start download $downloadValidUrl")
            try {
                client.getOkHttpInstance()
                    .download(
                        downloadValidUrl,
                        taskChunk.chunkStart,
                        taskChunk.chunkStart + taskChunk.chunkSize,
                        downloadTempFile
                    )
            } catch (e: Exception) {
                if (e is AliyunpanExceedMaxConcurrencyException) {
                    // 下载限流 是已知的执行异常
                    Thread.sleep(500)
                    if (downloadTask.isCancel()) {
                        throw InterruptedException("download is cancel")
                    }
                    Thread.sleep(500)
                }
                throw e
            }

            return taskChunk
        }
    }

    data class DownloadTask(
        private val downloader: AliyunpanDownloader,
        override val driveId: String,
        override val fileId: String,
        override val fileParentFileId: String,
        internal val expireSec: Int,
        internal val fileHashPath: String,
        internal val fileName: String,
        internal val fileSize: Long
    ) : BaseTask(driveId, fileId, fileParentFileId) {

        private var downloadUrl: String? = null
        private var expireSeconds: Long? = null

        override fun getTaskName(): String {
            return fileName
        }

        override fun start(): Boolean {
            return downloader.download(this)
        }

        internal fun recordDownloadUrl(url: String) {
            downloadUrl = url
            expireSeconds = (System.currentTimeMillis() / 1000) + (expireSec)
        }

        internal fun getValidDownloadUrl(): String? {
            if (downloadUrl.isNullOrEmpty()) {
                return null
            }

            val isExpired = expireSeconds?.let { (System.currentTimeMillis() / 1000) > it } ?: false
            if (isExpired) {
                return null
            }
            return downloadUrl
        }
    }

    companion object {

        private const val TAG = "AliyunpanDownloader"

        /**
         * Max Chunk Count
         * 最大 分片数
         */
        internal const val MAX_CHUNK_COUNT = 10000

        private const val DEFAULT_DOWNLOAD_TASK_SIZE = 2
    }
}

