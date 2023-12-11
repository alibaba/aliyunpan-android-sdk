package com.alicloud.databox.opensdk.auth

import android.content.Context
import com.alicloud.databox.opensdk.AliyunpanCredentials
import com.alicloud.databox.opensdk.utils.DataStoreControl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Aliyunpan secret credentials
 * secret方式 token有效期短 但是支持refresh_token
 * @property appSecret
 * @constructor
 *
 * @param context
 * @param appId
 */
internal class AliyunpanSecretCredentials(
    context: Context,
    appId: String,
    private val appSecret: String,
    identifier: String,
    baseAuthApi: String,
) : AliyunpanCredentials(context, appId, baseAuthApi) {

    private val dataStoreControl = DataStoreControl(context, "Secret", identifier)

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

    override fun getTokenRequest(authCode: String): Request {
        return Request.Builder()
            .url(buildUrl("oauth/access_token"))
            .post(
                JSONObject(
                    mapOf(
                        "client_id" to appId,
                        "client_secret" to appSecret,
                        "grant_type" to "authorization_code",
                        "code" to authCode,
                    )
                )
                    .toString()
                    .toRequestBody("application/json".toMediaType())
            )
            .build()
    }

    override fun getRefreshTokenRequest(): Request? {
        val authModel = authModel ?: return null
        if (authModel.supportRefresh()) {
            return Request.Builder()
                .url(buildUrl("oauth/access_token"))
                .post(
                    JSONObject(
                        mapOf(
                            "client_id" to appId,
                            "client_secret" to appSecret,
                            "grant_type" to "refresh_token",
                            "refresh_token" to authModel.refreshToken,
                        )
                    )
                        .toString()
                        .toRequestBody("application/json".toMediaType())
                )
                .build()
        }
        return null
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