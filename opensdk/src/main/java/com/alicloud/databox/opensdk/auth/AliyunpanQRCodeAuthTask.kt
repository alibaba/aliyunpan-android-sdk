package com.alicloud.databox.opensdk.auth

import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanUrlApi
import com.alicloud.databox.opensdk.Consumer
import okhttp3.Request
import java.util.concurrent.Executors

class AliyunpanQRCodeAuthTask(
    private val client: AliyunpanClient,
    private val urlApi: AliyunpanUrlApi,
    private val qrcodeUrl: String,
    private val sid: String
) {

    private var currentStatus: AliyunpanAuthorizeQRCodeStatus? = null

    private val handler = client.handler

    private val loopExecutor = Executors.newSingleThreadExecutor()

    private val stateChangeList = ArrayList<Consumer<AliyunpanAuthorizeQRCodeStatus>>()

    private val loopFetchAuth = object : Runnable {
        override fun run() {

            Thread.sleep(REQUEST_GAP_TOME)

            val request = Request.Builder()
                .url(urlApi.buildUrl("oauth/qrcode/${sid}/status"))
                .build()
            val response = client.sendSync(request)
            val jsonObject = response.data.asJSONObject()
            val status = jsonObject.optString("status")
            val authCode = jsonObject.optString("authCode")
            val authorizeQRCodeStatus = AliyunpanAuthorizeQRCodeStatus.values().find { it.stateName == status }
                ?: AliyunpanAuthorizeQRCodeStatus.WAIT_LOGIN

            postState(authorizeQRCodeStatus)

            if (AliyunpanAuthorizeQRCodeStatus.LOGIN_SUCCESS == authorizeQRCodeStatus) {
                client.fetchToken(authCode, null)
                loopExecutor.shutdown()
                return
            }

            if (AliyunpanAuthorizeQRCodeStatus.QRCODE_EXPIRED == authorizeQRCodeStatus) {
                loopExecutor.shutdown()
                return
            }

            loopExecutor.submit(this)
        }
    }

    private fun postState(authorizeQRCodeStatus: AliyunpanAuthorizeQRCodeStatus) {
        handler.post {
            if (currentStatus != authorizeQRCodeStatus) {
                for (consumer in stateChangeList) {
                    consumer.accept(authorizeQRCodeStatus)
                }
                currentStatus = authorizeQRCodeStatus
            }
        }
    }

    init {
        loopExecutor.submit(loopFetchAuth)
    }

    fun getQRCodeUrl(): String {
        return qrcodeUrl
    }

    fun addStateChange(onChange: Consumer<AliyunpanAuthorizeQRCodeStatus>) {
        stateChangeList.add(onChange)
    }

    fun removeStateChange(onChange: Consumer<AliyunpanAuthorizeQRCodeStatus>) {
        stateChangeList.remove(onChange)
    }

    companion object {
        private const val REQUEST_GAP_TOME = 1500L
    }
}