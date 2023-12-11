package com.alicloud.databox.opensdk.utils

import android.content.Context
import android.content.SharedPreferences

class DataStoreControl(
    private val context: Context,
    private val name: String,
    private val identifier: String
) {

    fun saveDataStore(key: String, value: String) {
        context.getSharedPreferences().edit()
            .putString(getIdKey(key), value)
            .apply()
    }

    fun saveDataStore(map: Map<String, Any>) {
        context.getSharedPreferences().edit()
            .apply {
                for (entry in map) {
                    putString(getIdKey(entry.key), entry.value.toString())
                }
            }
            .apply()
    }

    fun clearAll() {
        context.getSharedPreferences().edit()
            .clear()
            .apply()
    }

    private fun Context.getSharedPreferences(): SharedPreferences =
        getSharedPreferences("YunpanSdk-${name}.sp", Context.MODE_PRIVATE)

    fun getDataStore(key: String, defValue: String? = null): String? {
        return context.getSharedPreferences().getString(getIdKey(key), defValue)
    }

    private fun getIdKey(key: String) = "${identifier}_${key}"
}