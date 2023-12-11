package com.alicloud.databox.opensdk.kotlin

import com.alicloud.databox.opensdk.AliyunpanBaseClient
import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanClientConfig
import com.alicloud.databox.opensdk.AliyunpanScope
import com.alicloud.databox.opensdk.LLogger
import com.alicloud.databox.opensdk.ResultResponse
import okhttp3.Request
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AliyunpanClient(private val client: AliyunpanClient) : AliyunpanBaseClient by client {

    @Throws(Exception::class)
    suspend fun oauth() {
        return suspendCoroutine { continuation ->
            client.oauth({ continuation.resume(it) }, { continuation.resumeWithException(it) })
        }
    }

    @Throws(Exception::class)
    suspend fun send(command: AliyunpanScope): ResultResponse {
        return suspendCoroutine { continuation ->
            client.send(command,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Throws(Exception::class)
    suspend fun send(request: Request): ResultResponse {
        return suspendCoroutine { continuation ->
            client.send(request,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    companion object {

        fun init(config: AliyunpanClientConfig) = AliyunpanClient(AliyunpanClient.init(config)).also {
            LLogger.log(AliyunpanClient.TAG, "kotlin init")
        }
    }
}