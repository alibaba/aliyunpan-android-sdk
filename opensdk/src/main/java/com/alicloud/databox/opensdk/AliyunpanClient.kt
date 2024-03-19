package com.alicloud.databox.opensdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.alicloud.databox.opensdk.AliyunpanException.Companion.buildError
import com.alicloud.databox.opensdk.auth.AliyunpanQRCodeAuthTask
import com.alicloud.databox.opensdk.http.OKHttpHelper
import com.alicloud.databox.opensdk.http.OKHttpHelper.enqueue
import com.alicloud.databox.opensdk.http.OKHttpHelper.execute
import com.alicloud.databox.opensdk.http.TokenAuthenticator
import com.alicloud.databox.opensdk.io.AliyunpanDownloader
import com.alicloud.databox.opensdk.io.AliyunpanUploader
import com.alicloud.databox.opensdk.io.BaseTask
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AliyunpanClient private constructor(private val config: AliyunpanClientConfig) : AliyunpanBaseClient,
    TokenAuthenticator.TokenAuthenticatorConfig {

    internal val handler = Handler(Looper.myLooper()!!)

    private val downloader: AliyunpanDownloader? by lazy {
        val downloadFolderPath = config.downloadFolderPath
        if (downloadFolderPath.isEmpty()) {
            null
        } else {
            AliyunpanDownloader(
                this,
                downloadFolderPath
            )
        }
    }

    private val uploader: AliyunpanUploader by lazy {
        AliyunpanUploader(this, config.credentials)
    }

    private val okHttpInstance = OKHttpHelper.buildOKHttpClient(this, config)

    init {
        val context = config.context
        val credentials = config.credentials
        if (credentials.preCheckTokenValid()) {
            AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_LOGIN_SUCCESS)
        } else {
            AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_RESET_STATUS)
        }
    }

    override fun clearOauth() {
        AliyunpanBroadcastHelper.sentBroadcast(config.context, AliyunpanAction.NOTIFY_RESET_STATUS)
        config.credentials.clearToken()
    }

    fun oauthQRCode(onSuccess: Consumer<AliyunpanQRCodeAuthTask>, onFailure: Consumer<Exception>) {
        val credentials = config.credentials
        val scopes = config.scope.split(",")
        val requestJson = credentials.getOAuthQRCodeRequest(scopes)
        if (requestJson != null) {
            oauthQRCode(requestJson, onSuccess, onFailure)
            return
        }

        credentials.getOAuthQRCodeRequest(scopes) { value ->
            handler.post {
                if (value != null) {
                    oauthQRCode(value, onSuccess, onFailure)
                } else {
                    LLogger.log(
                        TAG,
                        "oauthQRCode TokenServer not implement getOAuthQRCodeRequest"
                    )
                    onFailure.accept(AliyunpanException.CODE_AUTH_QRCODE_ERROR.buildError("TokenServer not implement getOAuthQRCodeRequest"))
                }
            }
        }
    }

    private fun oauthQRCode(
        requestJson: JSONObject,
        onSuccess: Consumer<AliyunpanQRCodeAuthTask>,
        onFailure: Consumer<Exception>
    ) {
        val request = Request.Builder()
            .url(config.urlApi.buildUrl("oauth/authorize/qrcode"))
            .post(
                requestJson
                    .toString()
                    .toRequestBody("application/json".toMediaType())
            )
            .build()

        okHttpInstance.enqueue(request,
            handler,
            {
                val jsonObject = it.data.asJSONObject()
                val qrcodeUrl = jsonObject.optString("qrCodeUrl")
                val sid = jsonObject.optString("sid")
                onSuccess.accept(AliyunpanQRCodeAuthTask(this, config.urlApi, qrcodeUrl, sid))
            }, {
                LLogger.log(TAG, "oauth qrcode request failed", it)
                onFailure.accept(it)
            })
    }

    fun oauth(onSuccess: Consumer<Unit>, onFailure: Consumer<Exception>) {
        val context = config.context
        val credentials = config.credentials
        if (credentials.preCheckTokenValid()) {
            AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_LOGIN_SUCCESS)
            onSuccess.accept(null)
            return
        }

        if (!isInstanceYunpanApp()) {
            val exception = AliyunpanException.CODE_APP_NOT_INSTALL.buildError("yunpan app not install")
            LLogger.log(TAG, "oauth failed", exception)
            onFailure.accept(exception)
            return
        }

        val requestQuery = credentials.getOAuthRequest(config.scope)

        val request = Request.Builder()
            .url(config.urlApi.buildUrl("oauth/authorize", requestQuery))
            .build()

        okHttpInstance.enqueue(
            request,
            handler,
            {
                val jsonObject = it.data.asJSONObject()
                val redirectUri = jsonObject.optString("redirectUri")
                if (preCheckRedirect(redirectUri)) {
                    val exception =
                        AliyunpanException.CODE_AUTH_REDIRECT_INVALID.buildError("redirectUri is error uri = $redirectUri")
                    LLogger.log(TAG, "oauth redirectUri error", exception)
                    onFailure.accept(exception)
                    return@enqueue
                }

                if (startRedirectUri(context, redirectUri)) {
                    LLogger.log(TAG, "oauth redirectUri = $redirectUri")
                    onSuccess.accept(null)
                } else {
                    val exception = AliyunpanException.CODE_AUTH_REDIRECT_ERROR.buildError("start redirect failed")
                    LLogger.log(TAG, "oauth redirectUri error", exception)
                    onFailure.accept(exception)
                }
            }, {
                LLogger.log(TAG, "oauth request failed", it)
                onFailure.accept(it)
            })
    }

    private fun preCheckRedirect(redirectUri: String): Boolean {
        if (redirectUri.isEmpty()) {
            return false
        }
        return !redirectUri.startsWith("smartdrive")
    }

    private fun startRedirectUri(context: Context, redirectUri: String): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(redirectUri))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    override fun fetchToken(activity: Activity) {
        val intent = activity.intent
        val callbackCode = intent.getStringExtra(CALLBACK_CODE)
        val callbackError = intent.getStringExtra(CALLBACK_ERROR)
        fetchToken(callbackCode, callbackError)
        activity.finish()
    }

    internal fun fetchToken(code: String?, error: String?) {

        val context = config.context
        if (!error.isNullOrEmpty()) {
            LLogger.log(TAG, "fetchToken error = $error")
            AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_LOGIN_CANCEL, "$error")
            return
        }

        if (code.isNullOrEmpty()) {
            LLogger.log(TAG, "fetchToken code is null or empty")
            AliyunpanBroadcastHelper.sentBroadcast(
                context,
                AliyunpanAction.NOTIFY_LOGIN_FAILED,
                "code is null or empty"
            )
            return
        }
        val credentials = config.credentials
        val requestJson = credentials.getTokenRequest(code)

        if (requestJson != null) {
            val request = Request.Builder()
                .url(config.urlApi.buildUrl("oauth/access_token"))
                .post(
                    requestJson
                        .toString()
                        .toRequestBody("application/json".toMediaType())
                )
                .build()
            okHttpInstance.enqueue(request, handler, {
                try {
                    credentials.updateAccessToken(it.data.asJSONObject())
                    LLogger.log(TAG, "fetchToken getTokenRequest success")
                    AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_LOGIN_SUCCESS)
                } catch (e: Exception) {
                    LLogger.log(TAG, "fetchToken getTokenRequest failed", e)
                    AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_LOGIN_FAILED, e.message)
                }
            }, {
                LLogger.log(TAG, "fetchToken getTokenRequest failed", it)
                AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_LOGIN_FAILED, it.message)
            })
            return
        }

        credentials.getToken(code) { value ->
            handler.post {
                if (value != null) {
                    try {
                        credentials.updateAccessToken(value)
                        LLogger.log(TAG, "fetchToken getToken success")
                        AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_LOGIN_SUCCESS)
                    } catch (e: Exception) {
                        LLogger.log(TAG, "fetchToken getToken failed", e)
                        AliyunpanBroadcastHelper.sentBroadcast(
                            context,
                            AliyunpanAction.NOTIFY_LOGIN_FAILED,
                            e.message
                        )
                    }
                } else {
                    LLogger.log(TAG, "fetchToken TokenServer not implement getTokenRequest or getToken")
                    AliyunpanBroadcastHelper.sentBroadcast(
                        context,
                        AliyunpanAction.NOTIFY_LOGIN_FAILED,
                        "TokenServer not implement getTokenRequest or getToken"
                    )
                }
            }
        }
    }

    override fun authInvalid() {
        AliyunpanBroadcastHelper.sentBroadcast(config.context, AliyunpanAction.NOTIFY_LOGOUT)
    }

    /**
     * Send 发送请求
     *
     * @param scope 包装的请求作用域
     * @param onSuccess 主线程上的成功回调
     * @param onFailure 主线程上的失败回调
     */
    fun send(scope: AliyunpanScope, onSuccess: Consumer<ResultResponse>, onFailure: Consumer<Exception>) {
        val request = buildRequest(scope)
        if (request == null) {
            val exception = AliyunpanException.CODE_REQUEST_INVALID.buildError("build request failed")
            LLogger.log(TAG, "send failed", exception)
            onFailure.accept(exception)
            return
        }
        send(request, onSuccess, onFailure)
    }

    /**
     * Send 发送请求
     *
     * @param request 自定义的请求
     * @param onSuccess 主线程上的成功回调
     * @param onFailure 主线程上的失败回调
     */
    fun send(request: Request, onSuccess: Consumer<ResultResponse>, onFailure: Consumer<Exception>) {
        okHttpInstance.enqueue(request, handler, onSuccess, onFailure)
    }

    @Throws(Exception::class)
    internal fun sendSync(scope: AliyunpanScope): ResultResponse {
        val request = buildRequest(scope)
        if (request == null) {
            val exception = AliyunpanException.CODE_REQUEST_INVALID.buildError("build request failed")
            LLogger.log(TAG, "sendSync failed", exception)
            throw exception
        }
        return sendSync(request)
    }

    @Throws(Exception::class)
    internal fun sendSync(request: Request): ResultResponse {
        return okHttpInstance.execute(request)
    }

    fun buildDownload(
        driveId: String,
        fileId: String,
        onSuccess: Consumer<BaseTask>,
        onFailure: Consumer<Exception>
    ) {
        this.buildDownload(driveId, fileId, null, onSuccess, onFailure)
    }

    /**
     * Build download
     *
     * @param driveId
     * @param fileId
     * @param expireSec 下载地址过期时间 单位秒 默认900秒
     * @param onSuccess
     * @param onFailure
     */
    fun buildDownload(
        driveId: String,
        fileId: String,
        expireSec: Int? = null,
        onSuccess: Consumer<BaseTask>,
        onFailure: Consumer<Exception>
    ) {
        val downloader = downloader
        if (downloader == null) {
            onFailure.accept(AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("downloader is null, must be config download folder"))
            return
        }

        if (expireSec != null && expireSec <= 0) {
            onFailure.accept(AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("expireSec must be more than 0"))
            return
        }
        downloader.buildDownload(driveId, fileId, expireSec, onSuccess, onFailure)
    }

    fun buildUpload(
        driveId: String,
        loadFilePath: String,
        onSuccess: Consumer<BaseTask>,
        onFailure: Consumer<Exception>
    ) {
        this.buildUpload(driveId, loadFilePath, null, null, onSuccess, onFailure)
    }

    fun buildUpload(
        driveId: String,
        loadFilePath: String,
        parentFileId: String? = AliyunpanUploader.DEFAULT_UPLOAD_PARENT_FILE_ID,
        checkNameMode: String? = AliyunpanUploader.DEFAULT_UPLOAD_CHECK_NAME_MODE,
        onSuccess: Consumer<BaseTask>,
        onFailure: Consumer<Exception>
    ) {
        uploader.buildUpload(driveId, loadFilePath, parentFileId, checkNameMode, onSuccess, onFailure)
    }

    private fun buildRequest(scope: AliyunpanScope): Request? {
        val baseHttpUrl = config.urlApi.builder()

        return when (scope.getHttpMethod()) {
            "POST" -> {
                Request.Builder()
                    .url(
                        baseHttpUrl
                            .addPathSegments(scope.getApi())
                            .build()
                    )
                    .post(
                        JSONObject(scope.getRequest().filterValues { it != null })
                            .toString()
                            .toRequestBody("application/json".toMediaType())
                    )
                    .build()
            }

            "GET" -> {
                Request.Builder()
                    .url(
                        baseHttpUrl
                            .addPathSegments(scope.getApi())
                            .apply {
                                for (entry in scope.getRequest()) {
                                    val value = entry.value
                                    if (value != null) {
                                        addQueryParameter(entry.key, value.toString())
                                    }
                                }
                            }
                            .build()
                    )
                    .get()
                    .build()
            }

            else -> {
                null
            }
        }
    }

    override fun getOkHttpInstance() = okHttpInstance

    override fun isInstanceYunpanApp(): Boolean {
        val context = config.context
        val packageManager = context.packageManager
        return try {
            packageManager.getPackageInfo("com.alicloud.databox", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {

        private const val TAG = "AliyunpanClient"

        const val CALLBACK_CODE = "code"
        const val CALLBACK_ERROR = "error"

        @JvmStatic
        fun init(config: AliyunpanClientConfig) = AliyunpanClient(config).also {
            LLogger.log(TAG, "init")
        }
    }
}