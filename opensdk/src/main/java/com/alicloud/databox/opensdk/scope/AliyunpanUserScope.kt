package com.alicloud.databox.opensdk.scope

import com.alicloud.databox.opensdk.AliyunpanScope

interface AliyunpanUserScope {

    /**
     * 通过 access_token 获取用户信息
     *
     */

    class GetUsersInfo() : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "GET"
        }

        override fun getApi(): String {
            return "oauth/users/info"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf()
        }
    }

    /**
     * 获取用户信息和drive信息
     *
     */

    class GetDriveInfo() : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/user/getDriveInfo"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf()
        }
    }

    /**
     * 获取用户空间信息
     *
     */

    class GetSpaceInfo() : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "adrive/v1.0/user/getSpaceInfo"
        }

        override fun getRequest(): Map<String, Any?> {
            return emptyMap()
        }
    }

    /**
     * 获取用户vip信息
     *
     */

    class GetVipInfo() : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "POST"
        }

        override fun getApi(): String {
            return "v1.0/user/getVipInfo"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf()
        }
    }

    /**
     * 通过 access_token 获取用户权限信息
     *
     */

    class GetUsersScopes() : AliyunpanScope {
        override fun getHttpMethod(): String {
            return "GET"
        }

        override fun getApi(): String {
            return "oauth/users/scopes"
        }

        override fun getRequest(): Map<String, Any?> {
            return mapOf()
        }
    }
}