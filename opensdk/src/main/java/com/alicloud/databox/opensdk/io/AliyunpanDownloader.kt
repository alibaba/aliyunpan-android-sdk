package com.alicloud.databox.opensdk.io

import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanException
import com.alicloud.databox.opensdk.AliyunpanException.Companion.buildError
import com.alicloud.databox.opensdk.Consumer
import com.alicloud.databox.opensdk.LLogger
import com.alicloud.databox.opensdk.http.AliyunpanUrlExpiredException
import com.alicloud.databox.opensdk.http.OKHttpHelper.download
import com.alicloud.databox.opensdk.scope.AliyunpanFileScope
import java.io.File
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/**
 * Aliyunpan downloader
 * 下载器
 * 支持分片下载，并通知任务状态和进度
 * // TODO 支持断点续传 2023/12/19
 * @property client
 * @property downloadFolderPath
 * @constructor Create empty Aliyunpan downloader
 */
internal class AliyunpanDownloader(client: AliyunpanClient, private val downloadFolderPath: String) :
    AliyunpanIO<AliyunpanDownloader.DownloadTask>(client) {

    /**
     * Download group executor
     * 最大并行下载任务 线程池
     */
    private val downloadGroupExecutor =
        buildThreadGroupExecutor(DEFAULT_MAX_DOWNLOAD_TASK_RUNNING_SIZE, "download-group")

    private val downloadSubGroupTreadLocal = object : ThreadLocal<ExecutorService>() {

        override fun initialValue(): ExecutorService {
            return buildThreadGroupExecutor(DEFAULT_MAX_DOWNLOAD_SUB_RUNNING_SIZE, "download-sub-group")
        }
    }

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

                val validExpireSec = expireSec ?: DownloadTask.DEFAULT_EXPIRE_SECONDS

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

            if (downloadTask.isCancel()) {
                postAbort(downloadTask)
                return@submit
            }

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
            val allChunkList = lastAllChunkList ?: buildChunkList(downloadTask.fileSize)

            val doneChunkSet = mutableSetOf<TaskChunk>()

            if (lastDoneChunkSet != null) {
                doneChunkSet.addAll(lastDoneChunkSet)
            }

            var completedSize = if (doneChunkSet.isEmpty()) 0 else doneChunkSet.sumOf { it.chunkSize }

            postRunning(downloadTask, completedSize, downloadTask.fileSize)

            // 计算未完成分片
            val unDoneChunkList = if (lastDoneChunkSet.isNullOrEmpty()) {
                allChunkList
            } else {
                allChunkList.filterNot { lastDoneChunkSet.contains(it) }
            }

            val executorService = downloadSubGroupTreadLocal.get()

            if (executorService == null) {
                postFailed(downloadTask, AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("executorService is null"))
                return@submit
            }

            // 构造本次 下载分片任务集合
            val tasks =
                unDoneChunkList.map { DownloadRecursiveTask(downloadTask, it, downloadTempFile) }

            try {
                // 下载任务分组
                val taskGroup = tasks.chunked(DEFAULT_TASK_GROUP_CHUNK_SIZE)

                for (taskList in taskGroup) {

                    // 小分组提交 避免全量任务过多
                    val joinTasks = taskList.map { executorService.submit(it) }

                    for (joinTask in joinTasks) {
                        val doneChunk = joinTask.get()
                        completedSize += doneChunk.chunkSize
                        doneChunkSet.add(doneChunk)

                        postRunning(downloadTask, completedSize, downloadTask.fileSize)

                        if (downloadTask.isCancel()) {
                            postAbort(downloadTask)
                            return@submit
                        }
                    }
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

            client.getOkHttpInstance()
                .download(
                    downloadValidUrl,
                    taskChunk.chunkStart,
                    taskChunk.chunkStart + taskChunk.chunkSize,
                    downloadTempFile
                )
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

        companion object {

            internal const val DEFAULT_EXPIRE_SECONDS = 900
        }
    }

    companion object {

        private const val TAG = "AliyunpanDownloader"

        private const val DEFAULT_MAX_DOWNLOAD_TASK_RUNNING_SIZE = 2

        private const val DEFAULT_MAX_DOWNLOAD_SUB_RUNNING_SIZE = 3

        private const val DEFAULT_TASK_GROUP_CHUNK_SIZE = DEFAULT_MAX_DOWNLOAD_SUB_RUNNING_SIZE * 3
    }
}

