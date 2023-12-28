package com.alicloud.databox.demo

object SizeUtil {
    private val sizeUnit = arrayOf("Byte", "KB", "MB", "GB", "TB", "PB", "EB")

    /**
     * 将 byte 转成 字符串
     *
     * @param bytes 文件大小
     * @param approximate 是否近似值；不使用近似值，任何单位都保留2位小数；使用近似值，MB及以下的单位，不保留小数部分。
     */
    fun getFormatSize(bytes: Double, approximate: Boolean, whitespace: Boolean): String {
        if (bytes <= 0) {
            return "0 B"
        }
        var index = 0
        var sizeNumber = bytes
        while (sizeNumber >= 1024 && index < sizeUnit.size - 1) {
            sizeNumber /= 1024
            index++
        }
        return if (approximate) {
            if (index <= 2) { // MB 及以下的大小，不保留小数部分
                String.format("%.0f%s%s", sizeNumber, if (whitespace) " " else "", sizeUnit[index])
            } else {
                String.format("%.2f%s%s", sizeNumber, if (whitespace) " " else "", sizeUnit[index])
            }
        } else {
            String.format("%.2f%s%s", sizeNumber, if (whitespace) " " else "", sizeUnit[index])
        }
    }

    fun getFormatSize(bytes: Double, approximate: Boolean = false): String {
        return getFormatSize(bytes, approximate, true)
    }

    fun getFormatSize(bytes: Long, approximate: Boolean = false): String {
        return getFormatSize(bytes.toDouble(), approximate, true)
    }
}
