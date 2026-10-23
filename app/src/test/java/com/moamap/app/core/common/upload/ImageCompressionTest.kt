package com.moamap.app.core.common.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageCompressionTest {

    private val first = COMPRESSION_STEPS.first()

    /** iOS 와 같은 순서다. 한쪽만 바뀌면 같은 사진이 플랫폼마다 다르게 올라간다. */
    @Test
    fun `줄이는 순서는 화질을 먼저 낮추고 해상도를 마지막에 낮춘다`() {
        assertEquals(
            listOf(2048 to 80, 2048 to 70, 2048 to 60, 2048 to 50, 1600 to 50, 1280 to 50),
            COMPRESSION_STEPS.map { step -> step.maxLongEdge to step.quality },
        )
    }

    @Test
    fun `한도 안이면 그 결과를 쓴다`() {
        assertNull(nextCompressionStep(first, encodedSize = 1024, maxFileSize = MAX_PLACE_PHOTO_FILE_SIZE))
    }

    @Test
    fun `정확히 한도면 그 결과를 쓴다`() {
        assertNull(
            nextCompressionStep(first, encodedSize = MAX_PLACE_PHOTO_FILE_SIZE, maxFileSize = MAX_PLACE_PHOTO_FILE_SIZE),
        )
    }

    @Test
    fun `한도를 넘으면 바로 다음 단계로 간다`() {
        COMPRESSION_STEPS.zipWithNext().forEach { (current, expected) ->
            assertEquals(
                expected,
                nextCompressionStep(current, encodedSize = MAX_PLACE_PHOTO_FILE_SIZE + 1, maxFileSize = MAX_PLACE_PHOTO_FILE_SIZE),
            )
        }
    }

    /** 끝까지 넘을 때만 막는다. 안내 문구에 그 용도의 한도가 들어가야 한다. */
    @Test
    fun `마지막 단계로도 넘으면 그 용도의 한도로 TooLarge 이다`() {
        val error = assertThrows(ImageUploadException.TooLarge::class.java) {
            nextCompressionStep(
                COMPRESSION_STEPS.last(),
                encodedSize = MAX_REVIEW_PHOTO_FILE_SIZE + 1,
                maxFileSize = MAX_REVIEW_PHOTO_FILE_SIZE,
            )
        }
        assertEquals("사진 크기는 5MB 이하여야 해요", error.message)
    }

    /** 커버·프로필은 10MB 라 장소 한도로는 줄여야 할 크기도 첫 결과를 그대로 쓴다. */
    @Test
    fun `한도는 용도마다 따로 본다`() {
        val between = MAX_PLACE_PHOTO_FILE_SIZE + 1

        assertNull(nextCompressionStep(first, encodedSize = between, maxFileSize = MAX_IMAGE_FILE_SIZE))
        assertEquals(
            COMPRESSION_STEPS[1],
            nextCompressionStep(first, encodedSize = between, maxFileSize = MAX_PLACE_PHOTO_FILE_SIZE),
        )
    }

    /** 0 은 여기서 막지 않는다. 마지막 검사가 「읽지 못했어요」로 막아야 엉뚱한 크기 안내가 안 나간다. */
    @Test
    fun `빈 결과는 다음 단계로 넘기지 않는다`() {
        assertNull(nextCompressionStep(first, encodedSize = 0, maxFileSize = MAX_PLACE_PHOTO_FILE_SIZE))
    }

    @Test
    fun `정해진 단계가 아니면 거절한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            nextCompressionStep(
                CompressionStep(maxLongEdge = 999, quality = 99),
                encodedSize = MAX_PLACE_PHOTO_FILE_SIZE + 1,
                maxFileSize = MAX_PLACE_PHOTO_FILE_SIZE,
            )
        }
    }

    @Test
    fun `긴 변을 기준으로 비율을 지켜 줄인다`() {
        assertEquals(2048 to 1536, scaledSize(width = 4000, height = 3000, maxLongEdge = 2048))
        assertEquals(1536 to 2048, scaledSize(width = 3000, height = 4000, maxLongEdge = 2048))
        assertEquals(1280 to 960, scaledSize(width = 2048, height = 1536, maxLongEdge = 1280))
    }

    @Test
    fun `작은 사진은 키우지 않는다`() {
        assertEquals(800 to 600, scaledSize(width = 800, height = 600, maxLongEdge = 2048))
        assertEquals(2048 to 100, scaledSize(width = 2048, height = 100, maxLongEdge = 2048))
    }

    /** 아주 긴 파노라마도 짧은 변이 0 이 되면 Bitmap 을 만들 수 없다. */
    @Test
    fun `짧은 변은 1px 아래로 내려가지 않는다`() {
        assertEquals(2048 to 1, scaledSize(width = 100_000, height = 10, maxLongEdge = 2048))
    }

    @Test
    fun `줄여 읽을 때 정해진 해상도보다 작아지지 않는다`() {
        assertEquals(1, decodeSampleSize(width = 2048, height = 1536, maxLongEdge = 2048))
        assertEquals(1, decodeSampleSize(width = 4000, height = 3000, maxLongEdge = 2048))
        assertEquals(2, decodeSampleSize(width = 4096, height = 3072, maxLongEdge = 2048))
        assertEquals(4, decodeSampleSize(width = 12000, height = 9000, maxLongEdge = 2048))
        assertEquals(1, decodeSampleSize(width = 800, height = 600, maxLongEdge = 2048))
    }
}
