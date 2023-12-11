package com.alicloud.databox.opensdk.auth

import android.content.Context
import android.util.Base64
import com.alicloud.databox.opensdk.AliyunpanCredentials
import com.alicloud.databox.opensdk.utils.DataStoreControl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Aliyunpan PKCE credentials
 * PKCE方式 token有效期长30天 但是不支持refresh_token 只能再次授权
 * @constructor
 *
 * @param context
 * @param appId
 */
internal class AliyunpanPKCECredentials(
    context: Context,
    appId: String,
    identifier: String,
    baseAuthApi: String,
) : AliyunpanCredentials(context, appId, baseAuthApi) {

    private val dataStoreControl = DataStoreControl(context, "PKCE", identifier)

    private var authModel: AuthModel? = null

    private var codeVerifier = getRandomString()

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
                        "code_challenge" to getCodeChallenge(),
                        "code_challenge_method" to getCodeChallengeMethod(),
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
                        "grant_type" to "authorization_code",
                        "code" to authCode,
                        "code_verifier" to getCodeVerifier(),
                    )
                )
                    .toString()
                    .toRequestBody("application/json".toMediaType())
            )
            .build()
    }

    /**
     * Refresh token
     * 目前pkce不支持 refresh token操作
     * @return
     */
    override fun getRefreshTokenRequest(): Request? {
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

    private fun getCodeVerifier(): String {
        return codeVerifier
    }

    private fun getCodeChallenge(): String {
        val challengeMethod = getCodeChallengeMethod()
        if (METHOD_SHA_PLAIN == challengeMethod) {
            return getCodeVerifier()
        } else if (METHOD_SHA_256 == challengeMethod) {
            return getCodeVerifier().toByteArray().getSHA256().getBase64UrlSafe()
        }
        return ""
    }

    private fun getCodeChallengeMethod(): String {
        return METHOD_SHA_256
    }

    companion object {

        const val TAG = "PKCECredentials"

        const val METHOD_SHA_256 = "S256"
        const val METHOD_SHA_PLAIN = "plain"

        private fun getRandomString(): String {
            val secureRandom = SecureRandom()
            val codeByteArray = ByteArray(30)
            secureRandom.nextBytes(codeByteArray)
            return codeByteArray.toHex()
        }

        private fun ByteArray.getBase64UrlSafe(): String {
            return Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_WRAP)
        }

        private fun ByteArray.getSHA256(): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(this)
            return digest.digest()
        }

        private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
    }
}