package com.alicloud.databox.opensdk.auth

import android.content.Context
import com.alicloud.databox.opensdk.AliyunpanCredentials
import com.alicloud.databox.opensdk.AliyunpanTokenServer
import com.alicloud.databox.opensdk.utils.DataStoreControl
import okhttp3.Request
import org.json.JSONObject

/**
 * Aliyunpan secret credentials
 * Server服务方式 需要实现一种token获取方式
 * @see AliyunpanTokenServer
 * @constructor
 *
 * @param context
 * @param appId
 */
internal class AliyunpanServerCredentials(
    context: Context,
    appId: String,
    identifier: String,
    baseAuthApi: String,
    private val tokenServer: AliyunpanTokenServer,
) : AliyunpanCredentials(context, appId, baseAuthApi), AliyunpanTokenServer by tokenServer {

    private val dataStoreControl = DataStoreControl(context, "Server", identifier)

    private var authModel: AuthModel? = null

    init {
        authModel = AuthModel.parse(dataStoreControl)
    }

    override fun preCheckTokenValid(): Boolean {
        return authModel?.isValid() ?: false
    }

    override fun clearToken() {
        authModel?.clearStore(dataStoreControl)
        authModel = null
    }

    override fun getOAuthRequest(scope: String): Request {
        return Request.Builder()
            .url(
                buildUrl(
                    "oauth/authorize",
                    mapOf(
                        "client_id" to appId,
                        "bundle_id" to context.packageName,
                        "scope" to scope,
                        "redirect_uri" to "oob",
                        "response_type" to "code",
                        "source" to "app",
                    )
                )
            )
            .build()
    }

    override fun getAccessToken(): String? {
        return authModel?.accessToken
    }

    override fun updateAccessToken(jsonObject: JSONObject) {
        this.authModel = AuthModel.parse(jsonObject).apply {
            saveStore(dataStoreControl)
        }
    }
}