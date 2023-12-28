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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream

internal object OKHttpHelper {

    private const val NOTIFY_SIZE_THRESHOLD: Long = 512
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

    @Throws(AliyunpanException::class)
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
            if (!response.isSuccessful) {
                throw AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("download not success")
            }

            val body = response.body
            if (body == null) {
                throw AliyunpanException.CODE_DOWNLOAD_ERROR.buildError("download body is null")
            }

            inputStream = body.byteStream()
            randomAccessFile = BufferRandomAccessFile(downloadTempFile)

            var size: Int
            val buff = ByteArray(1024)

            randomAccessFile.seek(start)

            while (inputStream.read(buff).also { size = it } != -1) {
                randomAccessFile.write(buff, 0, size)
            }

            randomAccessFile.flushAndSync()
        } catch (e: IOException) {
            throw AliyunpanException.CODE_DOWNLOAD_ERROR.buildError(e.message ?: "download error")
        } finally {
            randomAccessFile?.close()
            response?.close()
            inputStream?.close()
        }
    }
}