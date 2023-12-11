package com.alicloud.databox.demo

import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date

object ViewHelper {

    fun TextView.appendWithTime(input: String) {
        val time = SimpleDateFormat("HH:mm:ss").format(Date())
        append(time)
        append(" : ")
        append(input)
        append("\n\n")
    }
}