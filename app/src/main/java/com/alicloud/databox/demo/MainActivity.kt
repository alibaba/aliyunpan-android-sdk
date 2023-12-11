package com.alicloud.databox.demo

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.alicloud.databox.demo.ViewHelper.appendWithTime
import com.alicloud.databox.opensdk.scope.AliyunpanFileScope
import com.alicloud.databox.opensdk.scope.AliyunpanUserScope

class MainActivity : AppCompatActivity() {

    private var defaultDriveId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvResult = findViewById<TextView>(R.id.tvResult)

        findViewById<View>(R.id.btnOAuth).setOnClickListener {
            startOAuth(tvResult)
        }

        findViewById<View>(R.id.btnOAuthClear).setOnClickListener {
            clearOAuth(tvResult)
        }

        findViewById<View>(R.id.btnUserInfo).setOnClickListener {
            getDriveInfo(tvResult)
        }

        findViewById<View>(R.id.btnFileListInfo).setOnClickListener {
            getFileList(defaultDriveId, tvResult)
        }

    }

    private fun startOAuth(tvResult: TextView) {
        AliyunpanApp.aliyunpanClient?.oauth({
            tvResult.appendWithTime("oauth start")
        }, {
            tvResult.appendWithTime("oauth failed: $it")
        })
    }

    private fun clearOAuth(tvResult: TextView) {
        AliyunpanApp.aliyunpanClient?.clearOauth()
        tvResult.appendWithTime("clear oauth")
    }

    private fun getDriveInfo(tvResult: TextView) {
        AliyunpanApp.aliyunpanClient?.send(AliyunpanUserScope.GetDriveInfo(),
            {
                tvResult.appendWithTime("GetDriveInfo success: $it")

                defaultDriveId = it.data.asJSONObject().optString("default_drive_id")
            }, {
                tvResult.appendWithTime("GetDriveInfo failed: $it")
            })
    }

    private fun getFileList(driveId: String?, tvResult: TextView) {
        if (driveId.isNullOrEmpty()) {
            return
        }
        AliyunpanApp.aliyunpanClient
            ?.send(AliyunpanFileScope.GetFileList(driveId, parentFileId = "root", fields = "*", limit = 2), {
                tvResult.appendWithTime("GetFileList success: $it")
            }, {
                tvResult.appendWithTime("GetFileList failed: $it")
            })
    }
}