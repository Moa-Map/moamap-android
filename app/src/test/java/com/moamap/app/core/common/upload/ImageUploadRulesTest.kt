package com.moamap.app.core.common.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageUploadRulesTest {

    /** 갤러리 선택기가 이 목록을 그대로 파생해 쓴다. 한쪽만 바뀌면 조용히 어긋난다. */
    @Test
    fun `허용 형식은 서버 계약 세 가지다`() {
        assertEquals(
            setOf("image/jpeg", "image/png", "image/webp"),
            ALLOWED_IMAGE_CONTENT_TYPES,
        )
    }

    @Test
    fun `서버가 받는 세 형식은 통과한다`() {
        validateImageUpload("image/jpeg", fileSize = 1024)
        validateImageUpload("image/png", fileSize = 1024)
        validateImageUpload("image/webp", fileSize = 1024)
    }

    /** ContentResolver 가 대문자로 주는 경우가 있다. 형식이 같은데 막으면 안 된다. */
    @Test
    fun `대문자로 와도 통과한다`() {
        validateImageUpload("IMAGE/JPEG", fileSize = 1024)
    }

    @Test
    fun `지원하지 않는 형식은 UnsupportedType 이다`() {
        assertThrows(ImageUploadException.UnsupportedType::class.java) {
            validateImageUpload("image/heic", fileSize = 1024)
        }
        assertThrows(ImageUploadException.UnsupportedType::class.java) {
            validateImageUpload("image/gif", fileSize = 1024)
        }
    }

    @Test
    fun `이미지가 아니면 UnsupportedType 이다`() {
        assertThrows(ImageUploadException.UnsupportedType::class.java) {
            validateImageUpload("application/pdf", fileSize = 1024)
        }
    }

    @Test
    fun `정확히 한도면 통과한다`() {
        validateImageUpload("image/jpeg", fileSize = MAX_IMAGE_FILE_SIZE)
    }

    @Test
    fun `한도를 넘으면 TooLarge 이다`() {
        assertThrows(ImageUploadException.TooLarge::class.java) {
            validateImageUpload("image/jpeg", fileSize = MAX_IMAGE_FILE_SIZE + 1)
        }
    }

    /** 크기를 알아내지 못하면 0 이 온다. 막으면 멀쩡한 사진을 못 올리므로 서버 판단에 맡긴다. */
    @Test
    fun `크기를 모르면 통과시킨다`() {
        validateImageUpload("image/jpeg", fileSize = 0)
    }

    /** 형식을 먼저 본다. 둘 다 틀렸을 때 크기 안내를 하면 고쳐도 또 막힌다. */
    @Test
    fun `형식과 크기가 모두 틀리면 형식을 먼저 알린다`() {
        assertThrows(ImageUploadException.UnsupportedType::class.java) {
            validateImageUpload("image/gif", fileSize = MAX_IMAGE_FILE_SIZE + 1)
        }
    }
}
