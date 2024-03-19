package com.alicloud.databox.opensdk.io

import org.junit.Assert
import org.junit.Test

class MessageDigestHelperTest {

    @Test
    fun testGetSHA256() {
        val result = MessageDigestHelper.getSHA256("123")
        Assert.assertEquals("a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3", result)
    }

    @Test
    fun testGetMD5() {
        val result = MessageDigestHelper.getMD5("123")
        Assert.assertEquals("202cb962ac59075b964b07152d234b70", result)
    }
}