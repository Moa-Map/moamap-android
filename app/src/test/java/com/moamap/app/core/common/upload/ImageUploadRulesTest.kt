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

    /** 서버가 장소 사진만 절반으로 잡고 있다. 두 한도가 같아지면 이 테스트가 먼저 깨진다. */
    @Test
    fun `장소 사진 한도는 커버 프로필의 절반이다`() {
        assertEquals(MAX_IMAGE_FILE_SIZE / 2, MAX_PLACE_PHOTO_FILE_SIZE)
    }

    @Test
    fun `장소 한도로 보면 정확히 한도까지 통과한다`() {
        validateImageUpload(
            "image/jpeg",
            fileSize = MAX_PLACE_PHOTO_FILE_SIZE,
            maxFileSize = MAX_PLACE_PHOTO_FILE_SIZE,
        )
    }

    @Test
    fun `장소 한도로 보면 그 위는 TooLarge 이다`() {
        assertThrows(ImageUploadException.TooLarge::class.java) {
            validateImageUpload(
                "image/jpeg",
                fileSize = MAX_PLACE_PHOTO_FILE_SIZE + 1,
                maxFileSize = MAX_PLACE_PHOTO_FILE_SIZE,
            )
        }
    }

    /** 커버 기준(10MB)으로 걸렀다면 통과했을 크기다. 한도를 넘기지 않으면 서버 400 을 그대로 받는다. */
    @Test
    fun `커버 한도로는 통과하는 크기도 장소 한도로는 막힌다`() {
        val between = MAX_PLACE_PHOTO_FILE_SIZE + 1

        validateImageUpload("image/jpeg", fileSize = between)

        assertThrows(ImageUploadException.TooLarge::class.java) {
            validateImageUpload(
                "image/jpeg",
                fileSize = between,
                maxFileSize = MAX_PLACE_PHOTO_FILE_SIZE,
            )
        }
    }

    /** 8MB 로 줄인 사진이 또 막히면 사용자는 무엇을 해야 할지 알 수 없다. */
    @Test
    fun `안내 문구에 실제 한도가 들어간다`() {
        assertEquals(
            "사진 크기는 5MB 이하여야 해요",
            ImageUploadException.TooLarge(MAX_PLACE_PHOTO_FILE_SIZE).message,
        )
        assertEquals(
            "사진 크기는 10MB 이하여야 해요",
            ImageUploadException.TooLarge(MAX_IMAGE_FILE_SIZE).message,
        )
    }
}
