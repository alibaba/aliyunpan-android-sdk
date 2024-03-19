package com.alicloud.databox.opensdk.io

import android.util.Base64
import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanCredentials
import com.alicloud.databox.opensdk.AliyunpanException
import com.alicloud.databox.opensdk.AliyunpanException.Companion.buildError
import com.alicloud.databox.opensdk.Consumer
import com.alicloud.databox.opensdk.LLogger
import com.alicloud.databox.opensdk.http.AliyunpanHttpException
import com.alicloud.databox.opensdk.http.AliyunpanUrlExpiredException
import com.alicloud.databox.opensdk.http.OKHttpHelper.upload
import com.alicloud.databox.opensdk.scope.AliyunpanFileScope
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.util.concurrent.Future
import kotlin.math.min

/**
 * Aliyunpan uploader
 * 上传器
 * 支持秒传 并通知任务状态和进度
 * @property client
 * @constructor Create empty Aliyunpan uploader
 */
internal class AliyunpanUploader(client: AliyunpanClient, private val credentials: AliyunpanCredentials) :
    AliyunpanIO<AliyunpanUploader.UploadTask>(client) {

    /**
     * Download group executor
     * 最大并行上传任务 线程池
     */
    private val uploadGroupExecutor =
        buildThreadGroupExecutor(DEFAULT_MAX_UPLOAD_TASK_RUNNING_SIZE, "upload-group")

    init {
        LLogger.log(TAG, "AliyunpanUploader init")
    }

    fun buildUpload(
        driveId: String,
        loadFilePath: String,
        parentFileId: String?,
        checkNameMode: String?,
        onSuccess: Consumer<BaseTask>,
        onFailure: Consumer<Exception>
    ) {
        buildUpload(driveId, loadFilePath, parentFileId, checkNameMode, null, onSuccess, onFailure)
    }

    private fun buildUpload(
        driveId: String,
        loadFilePath: String,
        parentFileId: String?,
        checkNameMode: String?,
        needPreHash: Boolean?,
        onSuccess: Consumer<BaseTask>,
        onFailure: Consumer<Exception>
    ) {
        val wrapOnFailure = Consumer<Exception> { t ->
            LLogger.log(TAG, "buildUpload failed", t)
            postFailure(onFailure, t)
        }

        if (driveId.isEmpty()) {
            wrapOnFailure.accept(AliyunpanException.CODE_UPLOAD_ERROR.buildError("driveId or fileId is empty"))
            return
        }

        val localFile = try {
            checkFileExists(loadFilePath)
        } catch (e: Exception) {
            wrapOnFailure.accept(e)
            return
        }

        val validFileName = localFile.name
        val validParentFileId = parentFileId ?: DEFAULT_UPLOAD_PARENT_FILE_ID
        val validCheckNameMode = checkNameMode ?: DEFAULT_UPLOAD_CHECK_NAME_MODE

        val fileSize = localFile.length()
        val validNeedPreHash = needPreHash ?: checkNeedPreHash(fileSize)
        val chunkList = buildChunkList(fileSize)

        // 运行中 去重
        for (uploadTask in runningTaskMap.keys()) {
            if (driveId == uploadTask.driveId && validParentFileId == uploadTask.fileParentFileId && loadFilePath == uploadTask.localFilePath) {
                LLogger.log(TAG, "buildDownload get running task")
                postSuccess(onSuccess, uploadTask)
                return
            }
        }

        uploadGroupExecutor.execute {
            try {
                val createFile = if (validNeedPreHash) {
                    AliyunpanFileScope.CreateFile(
                        driveId,
                        validParentFileId,
                        validFileName,
                        SUPPORT_FILE_TYPE,
                        validCheckNameMode,
                        size = fileSize,
                        partInfoList = chunkList.buildPartInfoList(),
                        preHash = MessageDigestHelper.getFilePreSHA1(loadFilePath)
                    )
                } else {
                    AliyunpanFileScope.CreateFile(
                        driveId,
                        validParentFileId,
                        validFileName,
                        SUPPORT_FILE_TYPE,
                        validCheckNameMode,
                        size = fileSize,
                        partInfoList = chunkList.buildPartInfoList(),
                        contentHashName = DEFAULT_UPLOAD_HASH_NAME,
                        contentHash = MessageDigestHelper.getFileSHA1(loadFilePath),
                        proofVersion = DEFAULT_UPLOAD_PROOF_VERSION,
                        proofCode = getProofCode(localFile)
                    )
                }

                val response = client.sendSync(createFile)

                val fileJson = response.data.asJSONObject()
                val fileId = fileJson.optString("file_id")
                val uploadId = fileJson.optString("upload_id")
                val fileName = fileJson.optString("file_name")
                val available = fileJson.optString("available")
                val exist = fileJson.optBoolean("exist")
                // 是否命中秒传
                val rapidUpload = fileJson.optBoolean("rapid_upload")
                val filePartInfoList = getFilePartInfoList(fileJson)

                if (exist) {
                    wrapOnFailure.accept(AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("file already exist"))
                    return@execute
                }

                LLogger.log(TAG, "buildUpload success")
                val uploadTask = UploadTask(
                    this,
                    driveId,
                    fileId,
                    validParentFileId,
                    UploadTask.DEFAULT_EXPIRE_SECONDS,
                    uploadId,
                    fileName,
                    fileSize,
                    loadFilePath,
                    rapidUpload
                ).apply {
                    recordUploadUrl(filePartInfoList)
                }
                postSuccess(onSuccess, uploadTask)
            } catch (e: Exception) {
                if (e is AliyunpanHttpException && e.code == CODE_PRE_HASH_MATCHED) {
                    // 大文件命中预秒传 开始尝试秒传
                    buildUpload(driveId, loadFilePath, parentFileId, checkNameMode, false, onSuccess, onFailure)
                } else {
                    wrapOnFailure.accept(e)
                }
            }
        }
    }

    private fun upload(uploadTask: UploadTask): Boolean {
        if (runningTaskMap.containsKey(uploadTask)) {
            return false
        }
        postWaiting(uploadTask)
        runningTaskMap[uploadTask] = uploadLoop(uploadTask, null, null)
        LLogger.log(TAG, "uploadLoop")
        return true
    }

    private fun uploadLoop(
        uploadTask: UploadTask,
        lastAllChunkList: List<TaskChunk>?,
        lastDoneChunkSet: Set<TaskChunk>?,
    ): Future<*> {
        return uploadGroupExecutor.submit {

            if (uploadTask.isCancel()) {
                postAbort(uploadTask)
                return@submit
            }

            // 秒传成功 直接完成
            if (uploadTask.rapidUpload) {
                postCompleted(uploadTask, uploadTask.localFilePath)
                return@submit
            }

            // 计算分片
            val allChunkList = lastAllChunkList ?: buildChunkList(uploadTask.fileSize)

            val doneChunkSet = mutableSetOf<TaskChunk>()

            if (lastDoneChunkSet != null) {
                doneChunkSet.addAll(lastDoneChunkSet)
            }

            var completedSize = if (doneChunkSet.isEmpty()) 0 else doneChunkSet.sumOf { it.chunkSize }

            val uploadUpUrlList = uploadTask.getValidUploadUrlList() ?: try {
                val filePartInfoList = fetchUploadUrl(uploadTask, allChunkList)
                uploadTask.recordUploadUrl(filePartInfoList)
                LLogger.log(TAG, "fetchUploadUrl success")
                filePartInfoList
            } catch (e: Exception) {
                LLogger.log(TAG, "fetchUploadUrl failed", e)
                postFailed(uploadTask, e)
                return@submit
            }

            if (uploadUpUrlList.isNullOrEmpty()) {
                LLogger.log(TAG, "upload url is empty")
                postFailed(uploadTask, AliyunpanException.CODE_UPLOAD_ERROR.buildError("uploadUpUrl is empty"))
                return@submit
            }

            val localFile = try {
                checkFileExists(uploadTask.localFilePath)
            } catch (e: Exception) {
                LLogger.log(TAG, "upload file check failed", e)
                postFailed(uploadTask, e)
                return@submit
            }

            postRunning(uploadTask, completedSize, uploadTask.fileSize)

            try {
                allChunkList.forEach {
                    val doneChunk = uploadChunk(uploadTask, it, localFile)
                    completedSize += doneChunk.chunkSize
                    doneChunkSet.add(doneChunk)

                    postRunning(uploadTask, completedSize, uploadTask.fileSize)

                    if (uploadTask.isCancel()) {
                        postAbort(uploadTask)
                        return@submit
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is InterruptedException -> {
                        // 取消
                        postAbort(uploadTask)
                        return@submit
                    }

                    is AliyunpanUrlExpiredException -> {
                        // 过期重试
                        postNext(uploadTask, allChunkList, lastDoneChunkSet)
                        return@submit
                    }

                    else -> {
                        postFailed(uploadTask, e)
                        return@submit
                    }
                }
            }

            // 比对分屏任务和完成任务集合
            if (allChunkList.size == doneChunkSet.size) {
                try {
                    completeUpload(uploadTask)
                } catch (e: Exception) {
                    postFailed(uploadTask, e)
                    return@submit
                }
                postCompleted(uploadTask, uploadTask.localFilePath)
            } else {
                postNext(uploadTask, allChunkList, doneChunkSet)
            }
        }
    }

    private fun getFilePartInfoList(fileJson: JSONObject): ArrayList<PartInfo>? {
        return fileJson.optJSONArray("part_info_list")?.let { list ->
            val result = ArrayList<PartInfo>()
            for (i in 0 until list.length()) {
                val filePartInfoItemJson = list.optJSONObject(i)
                result.add(
                    PartInfo(
                        filePartInfoItemJson.optInt("part_number"),
                        filePartInfoItemJson.optString("upload_url"),
                    )
                )
            }
            result
        }
    }

    private fun checkFileExists(loadFilePath: String): File {
        val file = File(loadFilePath)
        if (file.exists()) {
            if (file.isFile) {
                return file
            } else {
                throw AliyunpanException.CODE_UPLOAD_ERROR.buildError("uploadFilePath is not a file")
            }
        }
        throw AliyunpanException.CODE_UPLOAD_ERROR.buildError("uploadFilePath is not exist")
    }

    /**
     * Build part info list
     * 分片序列号，从 1 开始
     */
    private fun List<TaskChunk>.buildPartInfoList() = JSONArray(this.map { mapOf("part_number" to it.chunkIndex + 1) })

    /**
     * Get proof code
     * 秒传存储证明
     * https://www.yuque.com/aliyundrive/zpfszx/ezlzok#hwSW3
     * @param localFile
     * @return
     */
    private fun getProofCode(localFile: File): String? {
        val accessToken = credentials.getAccessToken()
        if (accessToken == null) {
            throw AliyunpanException.CODE_UPLOAD_ERROR.buildError("access token is null")
        }

        val headHex = MessageDigestHelper.getMD5(accessToken).substring(0, 16)
        val rangeModel = BigInteger(headHex, 16)

        val rangeStart = rangeModel.mod(BigInteger.valueOf(localFile.length())).toLong()
        val rangeEnd = min(rangeStart + 8, localFile.length())

        val proofCodeSample = ByteArray((rangeEnd - rangeStart).toInt())
        FileInputStream(localFile).use { inputStream ->
            inputStream.skip(rangeStart)
            inputStream.read(proofCodeSample)
        }

        return Base64.encodeToString(proofCodeSample, Base64.NO_WRAP)
    }

    /**
     * Check need pre hash
     * 是否开启预秒传 为了节省大文件秒传计算
     * @param fileSize
     * @return
     */
    private fun checkNeedPreHash(fileSize: Long): Boolean {
        return fileSize >= PRE_HASH_SIZE_THRESHOLD
    }

    private fun postNext(
        uploadTask: UploadTask,
        allChunkList: List<TaskChunk>,
        doneChunkSet: Set<TaskChunk>?
    ) {
        handler.post {
            runningTaskMap[uploadTask] = uploadLoop(uploadTask, allChunkList, doneChunkSet)
            LLogger.log(TAG, "uploadLoop next")
        }
    }

    private fun uploadChunk(uploadTask: UploadTask, taskChunk: TaskChunk, localFile: File): TaskChunk {

        val uploadValidUrl = uploadTask.getValidUploadUrl(taskChunk.chunkIndex)

        if (uploadValidUrl == null) {
            throw AliyunpanUrlExpiredException(
                AliyunpanException.CODE_UPLOAD_ERROR,
                "upload valid url is null or empty"
            )
        }

        if (uploadTask.isCancel()) {
            throw InterruptedException("upload is cancel")
        }

        client.getOkHttpInstance()
            .upload(
                uploadValidUrl.partUploadUrl,
                taskChunk.chunkStart,
                taskChunk.chunkStart + taskChunk.chunkSize,
                localFile
            )
        return taskChunk
    }

    @Throws(Exception::class)
    private fun fetchUploadUrl(uploadTask: UploadTask, chunkList: List<TaskChunk>): List<PartInfo>? {
        val resultResponse = client.sendSync(
            AliyunpanFileScope.GetUploadURL(
                uploadTask.driveId,
                uploadTask.fileId,
                uploadTask.uploadId,
                chunkList.buildPartInfoList()
            )
        )
        return getFilePartInfoList(resultResponse.data.asJSONObject())
    }

    @Throws(Exception::class)
    private fun completeUpload(uploadTask: UploadTask) {
        client.sendSync(
            AliyunpanFileScope.CompleteUpload(
                uploadTask.driveId,
                uploadTask.fileId,
                uploadTask.uploadId
            )
        )
    }

    internal data class PartInfo(
        val partNumber: Int,
        val partUploadUrl: String,
    )

    data class UploadTask(
        private val uploader: AliyunpanUploader,
        override val driveId: String,
        override val fileId: String,
        override val fileParentFileId: String,
        internal val expireSec: Int,
        internal val uploadId: String,
        internal val fileName: String,
        internal val fileSize: Long,
        internal val localFilePath: String,
        internal val rapidUpload: Boolean,
    ) : BaseTask(driveId, fileId, fileParentFileId) {

        private var partInfoList: List<PartInfo>? = null
        private var expireSeconds: Long? = null

        override fun getTaskName(): String {
            return fileName
        }

        override fun start(): Boolean {
            return uploader.upload(this)
        }

        internal fun recordUploadUrl(list: List<PartInfo>?) {
            partInfoList = list
            expireSeconds = (System.currentTimeMillis() / 1000) + (expireSec)
        }

        internal fun getValidUploadUrlList(): List<PartInfo>? {
            if (partInfoList.isNullOrEmpty()) {
                return null
            }

            val isExpired = expireSeconds?.let { (System.currentTimeMillis() / 1000) > it } ?: false
            if (isExpired) {
                return null
            }

            return partInfoList
        }

        internal fun getValidUploadUrl(index: Int): PartInfo? {
            return getValidUploadUrlList()?.get(index)
        }

        companion object {

            /**
             * Default Expire Seconds
             * 上传地址有效期 1小时
             */
            internal const val DEFAULT_EXPIRE_SECONDS = 3600
        }
    }

    companion object {

        const val TAG = "AliyunpanUploader"

        private const val DEFAULT_MAX_UPLOAD_TASK_RUNNING_SIZE = 2

        const val DEFAULT_UPLOAD_PARENT_FILE_ID = "root"

        const val UPLOAD_CHECK_NAME_MODE_AUTO_RENAME = "auto_rename" // 自动重命名
        const val UPLOAD_CHECK_NAME_MODE_REFUSE = "refuse" // 同名不创建
        const val UPLOAD_CHECK_NAME_MODE_IGNORE = "ignore" // 同名文件可创建
        const val DEFAULT_UPLOAD_CHECK_NAME_MODE = UPLOAD_CHECK_NAME_MODE_AUTO_RENAME

        private const val PRE_HASH_SIZE_THRESHOLD = 500 * 1024L

        private const val CODE_PRE_HASH_MATCHED = "PreHashMatched"

        private const val DEFAULT_UPLOAD_HASH_NAME = "sha1"
        private const val DEFAULT_UPLOAD_PROOF_VERSION = "v1"
    }
}