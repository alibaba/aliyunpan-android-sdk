package com.alicloud.databox.opensdk.io

import org.junit.Assert
import org.junit.Test

class AliyunpanIOTest {

    @Test
    fun testBuildDownloadChunkList() {
        testBuildChunkList(AliyunpanDownloader.MAX_CHUNK_COUNT)
    }

    @Test
    fun testBuildUploadChunkList() {
        testBuildChunkList(AliyunpanUploader.MAX_CHUNK_COUNT)
    }

    private fun testBuildChunkList(maxChunkSize: Int) {
        Assert.assertEquals(1, AliyunpanIO.buildChunkList(MAX_CHUNK_SIZE - 1, maxChunkSize).size)

        Assert.assertEquals(2, AliyunpanIO.buildChunkList(MAX_CHUNK_SIZE * 2, maxChunkSize).size)

        Assert.assertEquals(
            2,
            AliyunpanIO.buildChunkList(MAX_CHUNK_SIZE * 2 + 1, maxChunkSize).size
        )

        Assert.assertEquals(
            maxChunkSize,
            AliyunpanIO.buildChunkList(
                (maxChunkSize * MAX_CHUNK_SIZE) + 1,
                maxChunkSize
            ).size
        )
    }

    companion object {
        private const val MAX_CHUNK_SIZE = AliyunpanIO.MAX_CHUNK_SIZE
    }
}