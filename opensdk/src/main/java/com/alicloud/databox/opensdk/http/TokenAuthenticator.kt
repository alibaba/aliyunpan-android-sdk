package com.alicloud.databox.opensdk.http

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

internal class TokenAuthenticator(private val config: TokenAuthenticatorConfig) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        config.authInvalid()
        return null
    }

    interface TokenAuthenticatorConfig {
        fun authInvalid()
    }
}

