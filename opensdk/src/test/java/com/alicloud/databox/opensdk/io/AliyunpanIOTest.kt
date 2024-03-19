package com.alicloud.databox.opensdk.io

import org.junit.Assert
import org.junit.Test

class AliyunpanIOTest {

    @Test
    fun testBuildChunkListLess() {
        val buildChunkList = AliyunpanIO.buildChunkList(MAX_CHUNK_SIZE - 1)
        Assert.assertEquals(1, buildChunkList.size)
    }

    @Test
    fun testBuildChunkListExactly() {
        val buildChunkList = AliyunpanIO.buildChunkList(MAX_CHUNK_SIZE * 2)
        Assert.assertEquals(2, buildChunkList.size)
    }

    @Test
    fun testBuildChunkListMore() {
        val buildChunkList = AliyunpanIO.buildChunkList(MAX_CHUNK_SIZE * 2 + 1)
        Assert.assertEquals(2, buildChunkList.size)
    }

    @Test
    fun testBuildChunkListMoreBig() {
        val buildChunkList = AliyunpanIO.buildChunkList((MAX_CHUNK_COUNT * MAX_CHUNK_SIZE) + 1)
        Assert.assertEquals(MAX_CHUNK_COUNT, buildChunkList.size)
    }

    companion object {
        private const val MAX_CHUNK_SIZE = AliyunpanIO.MAX_CHUNK_SIZE
        private const val MAX_CHUNK_COUNT = AliyunpanIO.MAX_CHUNK_COUNT
    }
}