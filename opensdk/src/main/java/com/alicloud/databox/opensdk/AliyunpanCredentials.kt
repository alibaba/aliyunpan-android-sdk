package com.alicloud.databox.opensdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

abstract class AliyunpanCredentials(val context: Context, val appId: String) :
    AliyunpanTokenServer {

    abstract fun preCheckTokenValid(): Boolean

    abstract fun clearToken()

    abstract fun getOAuthRequest(scope: String): Map<String, String>

    abstract fun getAccessToken(): String?

    @Throws(Exception::class)
    abstract fun updateAccessToken(jsonObject: JSONObject)

    companion object {

        fun getPackageName(context: Context): String {
            return context.packageName
        }

        fun getPackageSignin(context: Context): String {
            val packageManager = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo =
                    packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = packageInfo.signingInfo
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                val packageInfo =
                    packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                packageInfo.signatures
            }

            if (signatures.isEmpty()) {
                return ""
            }
            val signature = signatures[0]
            return signature.toByteArray().getMD5().toHex()
        }

        fun getRandomString(): String {
            val secureRandom = SecureRandom()
            val codeByteArray = ByteArray(30)
            secureRandom.nextBytes(codeByteArray)
            return codeByteArray.toHex()
        }

        fun ByteArray.getBase64UrlSafe(): String {
            return Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_WRAP)
        }

        fun ByteArray.getSHA256(): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(this)
            return digest.digest()
        }

        fun ByteArray.getMD5(): ByteArray {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(this)
            return digest.digest()
        }

        fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
    }
}