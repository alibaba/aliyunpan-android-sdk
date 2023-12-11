package com.alicloud.databox.opensdk.auth

import com.alicloud.databox.opensdk.utils.DataStoreControl
import org.json.JSONObject

data class AuthModel private constructor(val accessToken: String, val refreshToken: String, val expired: Long) {

    fun isValid() = accessToken.isNotEmpty() && System.currentTimeMillis() < expired

    fun supportRefresh() = !refreshToken.isNullOrEmpty()

    fun saveStore(dataStoreControl: DataStoreControl) {
        dataStoreControl.saveDataStore(
            mapOf(
                ACCESS_TOKEN to accessToken,
                REFRESH_TOKEN to refreshToken,
                EXPIRED to expired,
            )
        )
    }

    fun clearStore(dataStoreControl: DataStoreControl) {
        dataStoreControl.clearAll()
    }

    companion object {

        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val EXPIRES_IN = "expires_in"
        const val EXPIRED = "expired"

        fun parse(jsonObject: JSONObject): AuthModel {
            val accessToken = jsonObject.optString(ACCESS_TOKEN)
            val refreshToken = jsonObject.optString(REFRESH_TOKEN)
            val expiresIn = jsonObject.optLong(EXPIRES_IN)

            val expired = System.currentTimeMillis() + (expiresIn * 1000)
            return AuthModel(
                accessToken,
                refreshToken,
                expired
            )
        }

        fun parse(dataStoreControl: DataStoreControl): AuthModel? {
            val accessToken = dataStoreControl.getDataStore(ACCESS_TOKEN) ?: return null
            val refreshToken = dataStoreControl.getDataStore(REFRESH_TOKEN) ?: return null
            val expired = dataStoreControl.getDataStore(EXPIRED)?.let {
                try {
                    it.toLong()
                } catch (e: Exception) {
                    return null
                }
            } ?: return null

            return AuthModel(accessToken, refreshToken, expired)
        }
    }
}