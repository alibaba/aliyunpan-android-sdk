package com.alicloud.databox.opensdk.io

import org.junit.Assert
import org.junit.Test

class AliyunpanDownloaderTest {

    @Test
    fun testBuildChunkListLess() {
        val buildChunkList = AliyunpanDownloader.buildChunkList(DEFAULT_CHUNK_SIZE - 100)
        Assert.assertEquals(2, buildChunkList.size)
    }

    @Test
    fun testBuildChunkListExactly() {
        val buildChunkList = AliyunpanDownloader.buildChunkList(DEFAULT_CHUNK_SIZE * 2)
        Assert.assertEquals(2, buildChunkList.size)
    }

    @Test
    fun testBuildChunkListMore() {
        val buildChunkList = AliyunpanDownloader.buildChunkList(DEFAULT_CHUNK_SIZE * 2 + 100)
        Assert.assertEquals(3, buildChunkList.size)
    }

    companion object {
        private const val DEFAULT_CHUNK_SIZE = AliyunpanDownloader.DEFAULT_MAX_CHUNK_SIZE
    }
}