package com.alicloud.databox.opensdk.io

import java.io.FileInputStream
import java.security.MessageDigest

internal object MessageDigestHelper {

    private const val SHA1 = "SHA-1"
    private const val SHA256 = "SHA-256"
    private const val MD5 = "MD5"

    /**
     * Gen sha1
     * @param path 读取文件全量算出sha1
     * @return
     */
    @Throws(Exception::class)
    fun getFileSHA1(path: String): String? {
        if (path.isEmpty()) {
            return null
        }
        FileInputStream(path).use { inputStream ->
            val buffer = ByteArray(1024 * 100)
            val digest =
                MessageDigest.getInstance(SHA1)
            var numRead = 0
            while (numRead != -1) {
                numRead = inputStream.read(buffer)
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead)
                }
            }
            val sha1Bytes = digest.digest()
            return sha1Bytes.toHex()
        }
    }

    /**
     * Gen pre sha1
     * @param path 读取文件前1024字节 算出预备sha1
     * @return
     */
    @Throws(Exception::class)
    fun getFilePreSHA1(path: String): String? {
        if (path.isEmpty()) {
            return null
        }
        FileInputStream(path).use { inputStream ->
            val buffer = ByteArray(1024)
            val digest =
                MessageDigest.getInstance(SHA1)
            val numRead = inputStream.read(buffer)
            if (numRead > 0) {
                digest.update(buffer, 0, numRead)
            }
            val sha1Bytes = digest.digest()
            return sha1Bytes.toHex()
        }
    }

    @Throws(Exception::class)
    fun getMD5(input: String): String {
        return getMD5(input.toByteArray()).toHex()
    }

    @Throws(Exception::class)
    fun getMD5(source: ByteArray): ByteArray {
        val md = MessageDigest.getInstance(MD5)
        md.update(source)
        return md.digest()
    }

    @Throws(Exception::class)
    fun getSHA256(input: String): String {
        return getSHA256(input.toByteArray()).toHex()
    }

    @Throws(Exception::class)
    fun getSHA256(source: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance(SHA256)
        digest.update(source)
        return digest.digest()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}