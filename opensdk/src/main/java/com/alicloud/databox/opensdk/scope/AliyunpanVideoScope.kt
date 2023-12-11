package com.alicloud.databox.opensdk.scope

import com.alicloud.databox.opensdk.AliyunpanScope

interface AliyunpanVideoScope {

    /**
     * 获取文件播放详情
     * @property driveId drive id
     * @property fileId file_id
     * @property category live_transcoding 边转边播
     * @property getSubtitleInfo 默认true
     * @property templateId 默认所有类型，枚举 LD|SD|HD|FHD|QHD
     * @property urlExpireSec 单位秒，最长4小时，默认15分钟。
     * @property onlyVip 默认fale，为true，仅会员可以查看所有内容
     * @property withPlayCursor 是否获取视频的播放进度，默认为false
     */

    class GetVideoPreviewPlayInfo(
        private val driveId: String,
        private val fileId: String,
        private val category: String? = null,
        private val getSubtitleInfo: Boolean? = null,
        private val templateId: String? = null,
        private val urlExpireSec: Int? = null,
        private val onlyVip: Boolean? = null,
        private val withPlayCursor: Boolean? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/getVideoPreviewPlayInfo"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "file_id" to fileId,
                "category" to category,
                "get_subtitle_info" to getSubtitleInfo,
                "template_id" to templateId,
                "url_expire_sec" to urlExpireSec,
                "only_vip" to onlyVip,
                "with_play_cursor" to withPlayCursor
            )
        }
    }

    /**
     * 获取文件播放元数据
     * @property driveId drive id
     * @property fileId file_id
     * @property category live_transcoding 边转边播
     * @property templateId 默认所有类型，枚举 LD|SD|HD|FHD|QHD
     */

    class GetVideoPreviewPlayMeta(
        private val driveId: String,
        private val fileId: String,
        private val category: String? = null,
        private val templateId: String? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/getVideoPreviewPlayMeta"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "file_id" to fileId,
                "category" to category,
                "template_id" to templateId
            )
        }
    }

    /**
     * 更新播放进度
     * @property driveId drive id
     * @property fileId file_id
     * @property playCursor 播放进度，单位s，可为小数
     * @property duration 视频总时长，单位s，可为小数
     */

    class UpdateVideoRecord(
        private val driveId: String,
        private val fileId: String,
        private val playCursor: String,
        private val duration: String? = null
    ) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/openFile/video/updateRecord"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf(
                "drive_id" to driveId,
                "file_id" to fileId,
                "play_cursor" to playCursor,
                "duration" to duration
            )
        }
    }

    /**
     * 获取最近播放列表
     * @property videoThumbnailWidth 缩略图宽度
     */

    class GetVideoRecentList(private val videoThumbnailWidth: Long? = null) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.1/openFile/video/recentList"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf("video_thumbnail_width" to videoThumbnailWidth)
        }
    }
}