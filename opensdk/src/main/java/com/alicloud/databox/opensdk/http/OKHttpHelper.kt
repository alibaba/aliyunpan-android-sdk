package com.alicloud.databox.opensdk.http

import android.os.Handler
import android.os.Looper
import com.alicloud.databox.opensdk.BuildConfig
import com.alicloud.databox.opensdk.Consumer
import com.alicloud.databox.opensdk.ResultResponse
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException

internal object OKHttpHelper {
    fun buildOKHttpClient(
        authenticatorConfig: TokenAuthenticator.TokenAuthenticatorConfig,
        httpHeaderConfig: HttpHeaderInterceptor.HttpHeaderConfig
    ) = OkHttpClient.Builder()
        .authenticator(TokenAuthenticator(authenticatorConfig))
        .addInterceptor(HttpHeaderInterceptor(httpHeaderConfig))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            }
        }
        .build()

    fun OkHttpClient.enqueue(
        request: Request,
        handler: Handler,
        onSuccess: Consumer<ResultResponse>,
        onFailure: Consumer<Exception>
    ) {
        this.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post { onFailure.accept(e) }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val resultResponse = buildResultResponse(response)
                    handler.post { onSuccess.accept(resultResponse) }
                } catch (e: Exception) {
                    handler.post { onFailure.accept(e) }
                }
            }
        })
    }

    fun buildAHttpException(response: Response): AliyunpanHttpException {
        val bodyString = response.body?.string()
        val jsonError = bodyString?.let { JSONObject(it) }
        return AliyunpanHttpException(
            jsonError?.optString("code") ?: response.code.toString(),
            jsonError?.optString("message") ?: ""
        )
    }

    fun buildResultResponse(response: Response): ResultResponse {
        if (response.isSuccessful) {
            val bytes = response.body?.bytes()
            return ResultResponse(
                response.code,
                ResultResponse.Data(bytes ?: ByteArray(0))
            )
        }
        throw buildAHttpException(response)
    }
}