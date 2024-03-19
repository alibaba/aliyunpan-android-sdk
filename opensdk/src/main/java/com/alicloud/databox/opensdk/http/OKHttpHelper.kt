package com.alicloud.databox.opensdk.http

import android.os.Handler
import androidx.annotation.WorkerThread
import com.alicloud.databox.opensdk.AliyunpanException
import com.alicloud.databox.opensdk.AliyunpanException.Companion.buildError
import com.alicloud.databox.opensdk.BuildConfig
import com.alicloud.databox.opensdk.Consumer
import com.alicloud.databox.opensdk.ResultResponse
import com.alicloud.databox.opensdk.io.BufferRandomAccessFile
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

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

    @Throws(Exception::class)
    fun OkHttpClient.execute(request: Request): ResultResponse {
        val response = this.newCall(request).execute()
        return buildResultResponse(response)
    }

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

    private fun buildHttpException(response: Response): AliyunpanHttpException {
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
        throw buildHttpException(response)
    }

    @Throws(Exception::class)
    @WorkerThread
    fun OkHttpClient.download(url: String, start: Long, end: Long, downloadTempFile: File) {
        // Range有效区间 在 0 至 (file size -1)
        val offsetEnd = end - 1
        val rangeHeader = "bytes=$start-$offsetEnd"
        val request: Request = Request.Builder()
            .addHeader("Range", rangeHeader)
            .url(url)
            .build()

        var response: Response? = null
        var inputStream: InputStream? = null
        var randomAccessFile: BufferRandomAccessFile? = null

        try {
            response = this.newCall(request).execute()

            val code = response.code
            val responseBody = response.body
            if (response.isSuccessful && responseBody != null) {
                inputStream = responseBody.byteStream()
                randomAccessFile = BufferRandomAccessFile(downloadTempFile)

                var size: Int
                val buff = ByteArray(1024)

                randomAccessFile.seek(start)

                while (inputStream.read(buff).also { size = it } != -1) {
                    randomAccessFile.write(buff, 0, size)
                }

                randomAccessFile.flushAndSync()
            } else {
                val errorBodyString = responseBody?.string()
                if (code == 403 && errorBodyString != null && errorBodyString.contains("Request has expired.")) {
                    throw AliyunpanUrlExpiredException(AliyunpanException.CODE_DOWNLOAD_ERROR, "request has expired")
                } else {
                    throw AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("download not success")
                }
            }
        } finally {
            randomAccessFile?.close()
            response?.close()
            inputStream?.close()
        }
    }

    @Throws(Exception::class)
    @WorkerThread
    fun OkHttpClient.upload(url: String, start: Long, end: Long, uploadFile: File) {

        val requestBody = object : RequestBody() {

            private val BUFFER_SIZE = 1024

            override fun contentType(): MediaType? {
                return null
            }

            override fun contentLength(): Long {
                return end - start
            }

            override fun writeTo(sink: BufferedSink) {
                var accessFile: RandomAccessFile? = null
                var result = ByteArray(BUFFER_SIZE)
                try {
                    accessFile = RandomAccessFile(uploadFile, "r")
                    accessFile.seek(start)
                    var completedSize: Long = 0
                    while (completedSize < contentLength()) {
                        if (completedSize + BUFFER_SIZE > contentLength()) {
                            result = ByteArray((contentLength() - completedSize).toInt())
                        }
                        val size = accessFile.read(result)
                        sink.write(result, 0, size)
                        completedSize += size.toLong()
                    }
                } finally {
                    accessFile?.close()
                }
            }
        }

        val request: Request = Request.Builder()
            .url(url)
            .put(requestBody)
            .build()

        var response: Response? = null
        try {
            response = this.newCall(request).execute()

            val code = response.code
            if (code == 200 || code == 409) {
                return
            }
            if (code == 403) {
                throw AliyunpanException.CODE_UPLOAD_ERROR.buildError("upload url expired")
            }
        } finally {
            response?.close()
        }
    }
}