package com.alicloud.databox.demo.ypauth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alicloud.databox.demo.AliyunpanApp

class YPAuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AliyunpanApp.aliyunpanClient?.fetchToken(this)
    }
}