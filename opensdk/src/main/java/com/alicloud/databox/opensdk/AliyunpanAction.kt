package com.alicloud.databox.opensdk

enum class AliyunpanAction {
    //登录成功
    NOTIFY_LOGIN_SUCCESS,

    //登录失败
    NOTIFY_LOGIN_FAILED,

    //登录取消
    NOTIFY_LOGIN_CANCEL,

    //注销成功
    NOTIFY_LOGOUT,

    //refreshCookies
    NOTIFY_REFRESH_COOKIES,

    //登录状态重置
    NOTIFY_RESET_STATUS,
}