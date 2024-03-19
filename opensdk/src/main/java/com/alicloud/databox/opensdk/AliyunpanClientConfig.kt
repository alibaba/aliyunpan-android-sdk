package com.alicloud.databox.opensdk

import android.content.Context
import android.os.Build
import com.alicloud.databox.opensdk.auth.AliyunpanPKCECredentials
import com.alicloud.databox.opensdk.auth.AliyunpanServerCredentials
import com.alicloud.databox.opensdk.http.HttpHeaderInterceptor
import java.io.File

class AliyunpanClientConfig private constructor(
    internal val context: Context,
    internal val scope: String,
    internal val urlApi: AliyunpanUrlApi,
    internal val credentials: AliyunpanCredentials,
    internal val downloadFolderPath: String
) : HttpHeaderInterceptor.HttpHeaderConfig {

    private val userAgent: String by lazy {
        val context = context
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
        USER_AGENT_FORMAT.format(
            packageInfo.packageName.split(".").last(),
            packageInfo.versionName,
            packageInfo.packageName,
            getSdkBuild(),
            Build.VERSION.RELEASE,
            getSdkVersion()
        )
    }

    private fun getSdkVersion() = BuildConfig.VERSION

    private fun getSdkBuild() = "1"

    override fun getConfigUserAgent(): String {
        return userAgent
    }

    override fun getConfigAuthorization(): String? {
        return credentials.getAccessToken()
    }

    companion object {

        private const val USER_AGENT_FORMAT = "%s/%s (%s; build:%s; Android %s) AliyunpanSDK/%s"

        const val SCOPE_USER_BASE = "user:base"
        const val SCOPE_FILE_READ = "file:all:read"
        const val SCOPE_FILE_WRITE = "file:all:write"
        const val SCOPE_ALBUM_SHARED_READ = "album:shared:read"
        private const val SCOPE_SEPARATOR = ","
    }

    class Builder {

        private val context: Context
        private val appId: String

        /**
         * Identifier
         * 默认用户标识
         */
        private var identifier: String = "sdk_user"

        /**
         * Scope
         * 默认的权限域
         */
        private var scopes: List<String> = arrayListOf(SCOPE_USER_BASE, SCOPE_FILE_READ)

        private val urlApi = AliyunpanUrlApi.getUriApi()

        private var tokenServer: AliyunpanTokenServer? = null

        private var downloadFolderPath: String = ""

        constructor(context: Context, appId: String) {
            this.context = context.applicationContext
            this.appId = appId
        }

        /**
         * Scope
         * 更多请查看 https://www.yuque.com/aliyundrive/zpfszx/dspik0
         * @param scopes 申请的授权范围 多个权限用","分割 例如 "user:base,file:all:read"
         */
        fun scope(scopes: String) = apply { this.scopes = scopes.split(SCOPE_SEPARATOR) }

        fun scope(scopes: List<String>) = apply { this.scopes = scopes }

        fun appendScope(scope: String) = apply { this.scopes = this.scopes.toMutableList().also { it.add(scope) } }

        fun setIdentifier(identifier: String) = apply { this.identifier = identifier }

        fun tokenServer(tokenServer: AliyunpanTokenServer) = apply { this.tokenServer = tokenServer }

        /**
         * Down folder
         * 配置文件下载文件夹路径，下载时注意文件读写权限授予
         * @param downloadFolderPath 下载文件夹路径
         */
        fun downFolder(downloadFolderPath: String) = apply { this.downloadFolderPath = downloadFolderPath }

        /**
         * Down folder
         * 配置文件下载文件夹，下载时注意文件读写权限授予
         * @param downloadFolder 下载文件夹
         */
        fun downFolder(downloadFolder: File) = apply { this.downloadFolderPath = downloadFolder.absolutePath }

        fun build(): AliyunpanClientConfig {

            val aliyunpanTokenServer = tokenServer
            val credentials = if (aliyunpanTokenServer == null) {
                AliyunpanPKCECredentials(context, appId, identifier)
            } else {
                AliyunpanServerCredentials(context, appId, identifier, aliyunpanTokenServer)
            }

            return AliyunpanClientConfig(
                context,
                scopes.joinToString(SCOPE_SEPARATOR),
                urlApi,
                credentials,
                downloadFolderPath
            )
        }

        companion object {
        }
    }
}