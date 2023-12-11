package com.alicloud.databox.opensdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.alicloud.databox.opensdk.AliyunpanException.Companion.buildError
import com.alicloud.databox.opensdk.http.OKHttpHelper
import com.alicloud.databox.opensdk.http.OKHttpHelper.enqueue
import com.alicloud.databox.opensdk.http.OKHttpHelper.execute
import com.alicloud.databox.opensdk.http.TokenAuthenticator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AliyunpanClient private constructor(private val config: AliyunpanClientConfig) : AliyunpanBaseClient,
    TokenAuthenticator.TokenAuthenticatorConfig {

    private val okHttpInstance = OKHttpHelper.buildOKHttpClient(this, config)

    private val mutex = Mutex()

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

        okHttpInstance.enqueue(credentials.getOAuthRequest(config.scope),
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
        val callbackCode = intent.getStringExtra(AliyunpanConstant.CALLBACK_CODE)
        val callbackError = intent.getStringExtra(AliyunpanConstant.CALLBACK_ERROR)
        fetchToken(callbackCode, callbackError)
        activity.finish()
    }

    private fun fetchToken(code: String?, error: String?) {

        val context = config.context
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(""))
        if (!error.isNullOrEmpty()) {
            LLogger.log(TAG, "fetchToken error = $error")
            AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_LOGIN_CANCEL, "error = $error")
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
        okHttpInstance.enqueue(credentials.getTokenRequest(code),
            {
                try {
                    credentials.updateAccessToken(it.data.asJSONObject())
                    LLogger.log(TAG, "fetchToken success")
                    AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_LOGIN_SUCCESS)
                } catch (e: Exception) {
                    LLogger.log(TAG, "fetchToken failed", e)
                    AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_LOGIN_FAILED, e.message)
                }
            }, {
                LLogger.log(TAG, "fetchToken failed", it)
                AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_LOGIN_FAILED, it.message)
            })
    }

    override fun refreshToken(authorization: String?): String? {
        val credentials = config.credentials
        val context = config.context
        return runBlocking {
            mutex.withLock {
                val request = credentials.getRefreshTokenRequest()
                if (request == null) {
                    null
                } else {
                    val currentToken = config.getConfigAuthorization()
                    // 旧的token 需要刷新
                    if (currentToken == authorization) {
                        try {
                            val response = okHttpInstance.execute(request)
                            credentials.updateAccessToken(response.data.asJSONObject())
                            AliyunpanBroadcastHelper.sentBroadcast(context, AliyunpanAction.NOTIFY_REFRESH_COOKIES)
                        } catch (e: Exception) {
                            LLogger.log(TAG, "refreshToken execute", e)
                        }
                        config.getConfigAuthorization()
                    } else {
                        currentToken
                    }
                }
            }
        }
    }

    override fun oauthInvalid() {
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
        okHttpInstance.enqueue(request, onSuccess, onFailure)
    }

    private fun buildRequest(scope: AliyunpanScope): Request? {
        val baseHttpUrl = HttpUrl.Builder()
            .scheme("https")
            .host(config.baseApi)

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

        const val TAG = "AliyunpanClient"

        fun init(config: AliyunpanClientConfig) = AliyunpanClient(config).also {
            LLogger.log(TAG, "init")
        }
    }
}