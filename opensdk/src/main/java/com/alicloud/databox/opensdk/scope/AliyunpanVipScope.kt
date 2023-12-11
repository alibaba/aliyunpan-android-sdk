package com.alicloud.databox.opensdk.scope

import com.alicloud.databox.opensdk.AliyunpanScope

interface AliyunpanVipScope {

    /**
     * 开始试用付费功能
     *
     */

    class GetVipFeatureList() : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "GET"
        }

        override fun getApi(): String {
            return "business/v1.0/vip/feature/list"
        }

        override fun getRequest(): Map<String, Any?> {
            return emptyMap()
        }
    }

    /**
     * 开始试用付费功能
     * @property featureCode 付费功能。枚举列表
     */

    class GetVipFeatureTrial(private val featureCode: String) : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "business/v1.0/vip/feature/trial"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf("featureCode" to featureCode)
        }
    }
}