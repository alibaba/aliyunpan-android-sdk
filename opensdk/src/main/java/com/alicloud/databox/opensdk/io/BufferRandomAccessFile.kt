package com.alicloud.databox.opensdk.io

import com.alicloud.databox.opensdk.AliyunpanException
import com.alicloud.databox.opensdk.AliyunpanException.Companion.buildError
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * The BufferedOutputStream implemented using [RandomAccessFile].
 * 用于下载场景文件内容保存
 */
internal class BufferRandomAccessFile(file: File) {

    private var out: BufferedOutputStream? = null
    private var fd: FileDescriptor? = null
    private var randomAccess = RandomAccessFile(file, "rw")

    init {
        try {
            fd = randomAccess.fd
            out = BufferedOutputStream(FileOutputStream(randomAccess.fd))
        } catch (e: IOException) {
            throw AliyunpanException.CODE_DOWNLOAD_ERROR.buildError(e.message ?: "BufferRandomAccessFile init")
        }
    }

    @Throws(IOException::class)
    fun write(b: ByteArray?, off: Int, len: Int) {
        out?.write(b, off, len)
    }

    @Throws(IOException::class)
    fun flushAndSync() {
        out?.flush()
        fd?.sync()
    }

    @Throws(IOException::class)
    fun close() {
        out?.close()
        randomAccess.close()
    }

    @Throws(IOException::class)
    fun seek(offset: Long) {
        randomAccess.seek(offset)
    }
}
