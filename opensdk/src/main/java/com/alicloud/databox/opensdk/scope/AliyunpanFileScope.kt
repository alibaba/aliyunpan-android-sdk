package com.alicloud.databox.opensdk.scope

import com.alicloud.databox.opensdk.AliyunpanScope

interface AliyunpanFileScope {

    /**
     * 获取文件列表
     * @property driveId drive id
     * @property limit 返回文件数量，默认 50，最大 100
     * @property marker 分页标记
     * @property orderBy created_at, updated_at, name, size, name_enhanced
     * @property orderDirection DESC ASC
     * @property parentFileId 根目录为root
     * @property category 分类，目前有枚举：video | doc | audio | zip | others | image, 可任意组合，按照逗号分割，例如 video,doc,audio, image,doc
     * @property type all | file | folder， 默认所有类型, type为folder时，category不做检查
     * @property videoThumbnailTime 生成的视频缩略图截帧时间，单位ms，默认120000ms
     * @property videoThumbnailWidth 生成的视频缩略图宽度，默认480px
     * @property imageThumbnailWidth 生成的图片缩略图宽度，默认480px
     * @property fields 当填 * 时，返回文件所有字段
     */

    class GetFileList(
        private val driveId: String,
        private val limit: Int? = null,
        private val marker: String? = null,
        private val orderBy: String? = null,
        private val orderDirection: String? = null,
        private val parentFileId: String,
        private val category: String? = null,
        private val type: String? = null,
        private val videoThumbnailTime: Long? = null,
        private val videoThumbnailWidth: Long? = null,
        private val imageThumbnailWidth: Long? = null,
        private val fields: String? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/list"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "limit" to limit,
                "marker" to marker,
                "order_by" to orderBy,
                "order_direction" to orderDirection,
                "parent_file_id" to parentFileId,
                "category" to category,
                "type" to type,
                "video_thumbnail_time" to videoThumbnailTime,
                "video_thumbnail_width" to videoThumbnailWidth,
                "image_thumbnail_width" to imageThumbnailWidth,
                "fields" to fields
            )
        }
    }

    /**
     * 文件搜索
     * @property driveId drive id
     * @property limit 返回文件数量，默认 100，最大100
     * @property marker 分页标记
     * @property query 查询语句，样例：固定目录搜索，只搜索一级 parent_file_id = '123' 精确查询 name = '123' 模糊匹配 name match '123' 搜索指定后缀文件 file_extension = 'apk'  范围查询 created_at < '2019-01-14T00:00:00' 复合查询： type = 'folder' or name = '123' parent_file_id = 'root' and name = '123' and category = 'video'
     * @property orderBy created_at ASC | DESC updated_at ASC | DESC name ASC | DESC size ASC | DESC
     * @property videoThumbnailTime 生成的视频缩略图截帧时间，单位ms，默认120000ms
     * @property videoThumbnailWidth 生成的视频缩略图宽度，默认480px
     * @property imageThumbnailWidth 生成的图片缩略图宽度，默认480px
     * @property returnTotalCount 是否返回总数
     */

    class SearchFile(
        private val driveId: String,
        private val limit: Int? = null,
        private val marker: String? = null,
        private val query: String? = null,
        private val orderBy: String? = null,
        private val videoThumbnailTime: Long? = null,
        private val videoThumbnailWidth: Long? = null,
        private val imageThumbnailWidth: Long? = null,
        private val returnTotalCount: Boolean? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/search"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "limit" to limit,
                "marker" to marker,
                "query" to query,
                "order_by" to orderBy,
                "video_thumbnail_time" to videoThumbnailTime,
                "video_thumbnail_width" to videoThumbnailWidth,
                "image_thumbnail_width" to imageThumbnailWidth,
                "return_total_count" to returnTotalCount
            )
        }
    }

    /**
     * 获取收藏文件列表
     * @property driveId drive id
     * @property limit 默认100，最大 100
     * @property marker 分页标记
     * @property orderBy created_at, updated_at, name, size
     * @property videoThumbnailTime 生成的视频缩略图截帧时间，单位ms，默认120000ms
     * @property videoThumbnailWidth 生成的视频缩略图宽度，默认480px
     * @property imageThumbnailWidth 生成的图片缩略图宽度，默认480px
     * @property orderDirection DESC ASC
     * @property type file 或 folder  , 默认所有类型
     */

    class GetStarredList(
        private val driveId: String,
        private val limit: Int? = null,
        private val marker: String? = null,
        private val orderBy: String? = null,
        private val videoThumbnailTime: Long? = null,
        private val videoThumbnailWidth: Long? = null,
        private val imageThumbnailWidth: Long? = null,
        private val orderDirection: String? = null,
        private val type: String? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/starredList"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "limit" to limit,
                "marker" to marker,
                "order_by" to orderBy,
                "video_thumbnail_time" to videoThumbnailTime,
                "video_thumbnail_width" to videoThumbnailWidth,
                "image_thumbnail_width" to imageThumbnailWidth,
                "order_direction" to orderDirection,
                "type" to type
            )
        }
    }

    /**
     * 获取文件详情
     * @property driveId drive id
     * @property fileId file_id
     * @property videoThumbnailTime 生成的视频缩略图截帧时间，单位ms，默认120000ms
     * @property videoThumbnailWidth 生成的视频缩略图宽度，默认480px
     * @property imageThumbnailWidth 生成的图片缩略图宽度，默认480px
     */

    class GetFile(
        private val driveId: String,
        private val fileId: String,
        private val videoThumbnailTime: Long? = null,
        private val videoThumbnailWidth: Long? = null,
        private val imageThumbnailWidth: Long? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/get"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "file_id" to fileId,
                "video_thumbnail_time" to videoThumbnailTime,
                "video_thumbnail_width" to videoThumbnailWidth,
                "image_thumbnail_width" to imageThumbnailWidth
            )
        }
    }

    /**
     * 根据文件路径查找文件
     * @property driveId drive id
     * @property filePath file_path
     */

    class GetFileByPath(private val driveId: String, private val filePath: String) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/get_by_path"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf("drive_id" to driveId, "file_path" to filePath)
        }
    }

    /**
     * 批量获取文件详情
     * @property fileList
     */

    class BatchGet(private val fileList: String) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/batch/get"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf("file_list" to fileList)
        }
    }

    /**
     * 获取文件下载详情
     * @property driveId drive id
     * @property fileId file_id
     * @property expireSec 下载地址过期时间，单位为秒，默认为 900 秒, 最长4h（14400秒）
     */

    class GetFileGetDownloadUrl(
        private val driveId: String,
        private val fileId: String,
        private val expireSec: Int? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/getDownloadUrl"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf("drive_id" to driveId, "file_id" to fileId, "expire_sec" to expireSec)
        }
    }

    /**
     * 创建文件
     * @property driveId drive id
     * @property parentFileId 根目录为root
     * @property name 文件名称，按照 utf8 编码最长 1024 字节，不能以 / 结尾
     * @property type file | folder
     * @property checkNameMode auto_rename 自动重命名，存在并发问题, refuse 同名不创建, ignore 同名文件可创建
     * @property partInfoList 最大分片数量 10000
     * @property streamsInfo 仅上传livp格式的时候需要，常见场景不需要
     * @property preHash 针对大文件sha1计算非常耗时的情况， 可以先在读取文件的前1k的sha1， 如果前1k的sha1没有匹配的， 那么说明文件无法做秒传， 如果1ksha1有匹配再计算文件sha1进行秒传，这样有效边避免无效的sha1计算。
     * @property size 秒传必须, 文件大小，单位为 byte
     * @property contentHash 秒传必须, 文件内容 hash 值，需要根据 content_hash_name 指定的算法计算，当前都是sha1算法
     * @property contentHashName 秒传必须, 默认都是 sha1
     * @property proofCode 秒传必须
     * @property proofVersion 固定 v1
     * @property localCreatedAt 本地创建时间，格式yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     * @property localModifiedAt 本地修改时间，格式yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     */

    class CreateFile(
        private val driveId: String,
        private val parentFileId: String,
        private val name: String,
        private val type: String,
        private val checkNameMode: String,
        private val partInfoList: String? = null,
        private val streamsInfo: String? = null,
        private val preHash: String? = null,
        private val size: Long? = null,
        private val contentHash: String? = null,
        private val contentHashName: String? = null,
        private val proofCode: String? = null,
        private val proofVersion: String? = null,
        private val localCreatedAt: String? = null,
        private val localModifiedAt: String? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/create"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "parent_file_id" to parentFileId,
                "name" to name,
                "type" to type,
                "check_name_mode" to checkNameMode,
                "part_info_list" to partInfoList,
                "streams_info" to streamsInfo,
                "pre_hash" to preHash,
                "size" to size,
                "content_hash" to contentHash,
                "content_hash_name" to contentHashName,
                "proof_code" to proofCode,
                "proof_version" to proofVersion,
                "local_created_at" to localCreatedAt,
                "local_modified_at" to localModifiedAt
            )
        }
    }

    /**
     * 刷新获取上传地址
     * @property driveId drive id
     * @property fileId file_id
     * @property uploadId 文件创建获取的upload_id
     * @property partInfoList 分片信息列表
     */

    class GetUploadURL(
        private val driveId: String,
        private val fileId: String,
        private val uploadId: String,
        private val partInfoList: String? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/getUploadUrl"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "file_id" to fileId,
                "upload_id" to uploadId,
                "part_info_list" to partInfoList
            )
        }
    }

    /**
     * 列举已上传分片
     * @property driveId drive id
     * @property fileId file_id
     * @property uploadId 文件创建获取的upload_id
     * @property partNumberMarker
     */

    class ListUploadedParts(
        private val driveId: String,
        private val fileId: String,
        private val uploadId: String,
        private val partNumberMarker: String? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/listUploadedParts"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "file_id" to fileId,
                "upload_id" to uploadId,
                "part_number_marker" to partNumberMarker
            )
        }
    }

    /**
     * 标记文件上传完毕
     * @property driveId drive id
     * @property fileId file_id
     * @property uploadId 文件创建获取的upload_id
     */

    class CompleteUpload(private val driveId: String, private val fileId: String, private val uploadId: String) :
        AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/complete"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf("drive_id" to driveId, "file_id" to fileId, "upload_id" to uploadId)
        }
    }

    /**
     * 文件更新
     * @property driveId drive id
     * @property fileId file_id
     * @property name 新的文件名
     * @property checkNameMode auto_rename 自动重命名, refuse 同名不创建, ignore 同名文件可创建。 默认
     * @property starred 收藏 true，移除收藏 false
     */

    class UpdateFile(
        private val driveId: String,
        private val fileId: String,
        private val name: String? = null,
        private val checkNameMode: String? = null,
        private val starred: Boolean? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/update"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "file_id" to fileId,
                "name" to name,
                "check_name_mode" to checkNameMode,
                "starred" to starred
            )
        }
    }

    /**
     * 移动文件或文件夹
     * @property driveId 当前drive id
     * @property fileId file_id
     * @property toDriveId 目标drive，默认是当前drive_id 目前只能在当前drive操作
     * @property toParentFileId 父文件ID、根目录为 root
     * @property checkNameMode 同名文件处理模式，可选值如下：ignore：允许同名文件；auto_rename：当发现同名文件是，云端自动重命名。refuse：当云端存在同名文件时，拒绝创建新文件。默认为 refuse
     * @property newName 当云端存在同名文件时，使用的新名字
     */

    class MoveFile(
        private val driveId: String,
        private val fileId: String,
        private val toDriveId: String? = null,
        private val toParentFileId: String,
        private val checkNameMode: String? = null,
        private val newName: String? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/move"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "file_id" to fileId,
                "to_drive_id" to toDriveId,
                "to_parent_file_id" to toParentFileId,
                "check_name_mode" to checkNameMode,
                "new_name" to newName
            )
        }
    }

    /**
     * 复制文件或文件夹
     * @property driveId drive id
     * @property fileId file_id
     * @property toDriveId 目标drive，默认是当前drive_id
     * @property toParentFileId 父文件ID、根目录为 root
     * @property autoRename 当目标文件夹下存在同名文件时，是否自动重命名，默认为 false，默认允许同名文件
     */

    class CopyFile(
        private val driveId: String,
        private val fileId: String,
        private val toDriveId: String? = null,
        private val toParentFileId: String,
        private val autoRename: Boolean? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/copy"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "file_id" to fileId,
                "to_drive_id" to toDriveId,
                "to_parent_file_id" to toParentFileId,
                "auto_rename" to autoRename
            )
        }
    }

    /**
     * 放入回收站
     * @property driveId drive id
     * @property fileId file_id
     */

    class TrashFileToRecyclebin(private val driveId: String, private val fileId: String) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/recyclebin/trash"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf("drive_id" to driveId, "file_id" to fileId)
        }
    }

    /**
     * 删除文件
     * @property driveId drive id
     * @property fileId file_id
     */

    class DeleteFile(private val driveId: String, private val fileId: String) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/delete"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf("drive_id" to driveId, "file_id" to fileId)
        }
    }

    /**
     * 获取异步任务状态
     * @property asyncTaskId 异步任务ID
     */

    class GetAsyncTask(private val asyncTaskId: String) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/async_task/get"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf("async_task_id" to asyncTaskId)
        }
    }
}