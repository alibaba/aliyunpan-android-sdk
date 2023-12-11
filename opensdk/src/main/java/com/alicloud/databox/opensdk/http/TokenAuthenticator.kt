package com.alicloud.databox.opensdk.http

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(private val config: TokenAuthenticatorConfig) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        return when {
            response.retryCount > 2 -> null
            else -> response.createRefreshedTokenRequest()
        }
    }

    private val Response.retryCount: Int
        get() {
            var currentResponse = priorResponse
            var result = 0
            while (currentResponse != null) {
                result++
                currentResponse = currentResponse.priorResponse
            }
            return result
        }

    private fun Response.createRefreshedTokenRequest(): Request? {
        val refreshToken = config.refreshToken(request.header(HEADER_AUTHORIZATION))
        if (refreshToken.isNullOrEmpty()) {
            config.oauthInvalid()
            return null
        }
        return this.request
            .newBuilder()
            .header(HEADER_AUTHORIZATION, refreshToken)
            .build()
    }

    interface TokenAuthenticatorConfig {
        fun refreshToken(authorization: String?): String?

        fun oauthInvalid()
    }

    companion object {

        const val HEADER_AUTHORIZATION = "Authorization"
    }
}

