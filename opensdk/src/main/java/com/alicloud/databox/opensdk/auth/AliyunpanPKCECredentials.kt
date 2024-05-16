package com.alicloud.databox.opensdk.auth

import android.content.Context
import android.util.Base64
import com.alicloud.databox.opensdk.AliyunpanCredentials
import com.alicloud.databox.opensdk.io.MessageDigestHelper
import com.alicloud.databox.opensdk.utils.DataStoreControl
import org.json.JSONObject
import java.security.SecureRandom

/**
 * Aliyunpan PKCE credentials
 * PKCE方式 token有效期长30天
 * @constructor
 *
 * @param context
 * @param appId
 */
internal class AliyunpanPKCECredentials(
    context: Context,
    appId: String,
    identifier: String,
) : AliyunpanCredentials(context, appId) {

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

    override fun getOAuthRequest(scope: String): Map<String, String> {
        return mapOf(
            "client_id" to appId,
            "bundle_id" to context.packageName,
            "scope" to scope,
            "redirect_uri" to "oob",
            "response_type" to "code",
            "code_challenge" to getCodeChallenge(),
            "code_challenge_method" to getCodeChallengeMethod(),
        )
    }

    override fun getOAuthQRCodeRequest(scopes: List<String>): JSONObject? {
        return JSONObject(
            mapOf(
                "client_id" to appId,
                "bundle_id" to context.packageName,
                "scopes" to scopes,
                "source" to "app",
                "code_challenge" to getCodeChallenge(),
                "code_challenge_method" to getCodeChallengeMethod(),
            )
        )
    }

    override fun getAccessToken(): String? {
        return authModel?.accessToken
    }

    override fun updateAccessToken(jsonObject: JSONObject) {
        this.authModel = AuthModel.parse(jsonObject).apply {
            saveStore(dataStoreControl)
        }
    }

    override fun getTokenRequest(authCode: String): JSONObject {
        return JSONObject(
            mapOf(
                "client_id" to appId,
                "grant_type" to "authorization_code",
                "code" to authCode,
                "code_verifier" to getCodeVerifier(),
            )
        )
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
            return MessageDigestHelper.getSHA256(this)
        }

        private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
    }
}