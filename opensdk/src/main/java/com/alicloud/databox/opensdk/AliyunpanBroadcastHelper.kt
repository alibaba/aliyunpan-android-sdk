package com.alicloud.databox.opensdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

object AliyunpanBroadcastHelper {

    private const val TAG = "AliyunpanBroadcastHelper"

    private val filter: IntentFilter = IntentFilter()

    init {
        for (action in AliyunpanAction.entries) {
            filter.addAction(action.name)
        }
        filter.priority = 1000
    }

    fun registerReceiver(context: Context, receiver: BroadcastReceiver) {
        try {
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        } catch (e: Exception) {
            LLogger.log(TAG, "registerReceiver", e)
        }
    }

    fun unregisterReceiver(context: Context, receiver: BroadcastReceiver) {
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        } catch (e: Exception) {
            LLogger.log(TAG, "unregisterReceiver", e)
        }
    }

    internal fun sentBroadcast(context: Context, action: AliyunpanAction, message: String? = null) {
        val intent = Intent(action.name).apply {
            if (!message.isNullOrEmpty()) {
                putExtra("message", message)
            }
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}