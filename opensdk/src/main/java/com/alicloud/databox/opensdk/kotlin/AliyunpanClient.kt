package com.alicloud.databox.opensdk.kotlin

import com.alicloud.databox.opensdk.AliyunpanBaseClient
import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanClientConfig
import com.alicloud.databox.opensdk.AliyunpanScope
import com.alicloud.databox.opensdk.LLogger
import com.alicloud.databox.opensdk.ResultResponse
import com.alicloud.databox.opensdk.auth.AliyunpanQRCodeAuthTask
import com.alicloud.databox.opensdk.io.BaseTask
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
    suspend fun oauthQRCode(): AliyunpanQRCodeAuthTask {
        return suspendCoroutine { continuation ->
            client.oauthQRCode({ continuation.resume(it) }, { continuation.resumeWithException(it) })
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

    @Throws(Exception::class)
    suspend fun buildDownload(
        driveId: String,
        fileId: String,
        expireSec: Int? = null,
    ): BaseTask {
        return suspendCoroutine { continuation ->
            client.buildDownload(driveId, fileId, expireSec,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) })
        }
    }

    companion object {

        private const val TAG = "AliyunpanClient-kt"

        fun init(config: AliyunpanClientConfig) = AliyunpanClient(AliyunpanClient.init(config)).also {
            LLogger.log(TAG, "init")
        }
    }
}