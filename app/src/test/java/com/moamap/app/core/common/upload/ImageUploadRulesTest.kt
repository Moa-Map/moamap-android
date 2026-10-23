package com.moamap.app.core.common.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageUploadRulesTest {

    /** 발급 전 마지막 검사와 갤러리 선택기가 이 목록에서 파생해 쓴다. 한쪽만 바뀌면 조용히 어긋난다. */
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

    /** 직접 세어도 0 이면 읽을 수 없는 사진이다. 보내면 서버가 「크기 초과」로 거절해 엉뚱한 안내가 된다(이슈 #318). */
    @Test
    fun `크기를 읽지 못하면 막는다`() {
        assertThrows(ImageUploadException.Unreadable::class.java) {
            validateImageUpload("image/jpeg", fileSize = 0)
        }
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

    /** 줄이면서 JPEG 로 바꾸므로 서버가 받지 않는 HEIC 도 원본으로는 받는다. iOS 와 같다. */
    @Test
    fun `API 28 이상이면 HEIC 와 HEIF 원본을 받는다`() {
        requireUploadableSourceType("image/heic", sdkInt = 28)
        requireUploadableSourceType("image/heif", sdkInt = 28)
        requireUploadableSourceType("image/heic", sdkInt = 35)
    }

    /** ImageDecoder 가 없고 BitmapFactory 가 읽지 못하는 기기가 있다. 읽지 못했다고 하느니 형식을 알린다. */
    @Test
    fun `API 28 미만이면 HEIC 와 HEIF 원본은 UnsupportedType 이다`() {
        assertThrows(ImageUploadException.UnsupportedType::class.java) {
            requireUploadableSourceType("image/heic", sdkInt = 27)
        }
        assertThrows(ImageUploadException.UnsupportedType::class.java) {
            requireUploadableSourceType("image/heif", sdkInt = 24)
        }
    }

    @Test
    fun `서버가 받는 세 형식은 어느 버전에서나 원본으로 받는다`() {
        listOf(24, 27, 28, 35).forEach { sdkInt ->
            ALLOWED_IMAGE_CONTENT_TYPES.forEach { type -> requireUploadableSourceType(type, sdkInt) }
        }
    }

    /** 읽을 수는 있어도 첫 장면만 조용히 올라간다. */
    @Test
    fun `GIF 원본은 어느 버전에서나 UnsupportedType 이다`() {
        listOf(24, 28, 35).forEach { sdkInt ->
            assertThrows(ImageUploadException.UnsupportedType::class.java) {
                requireUploadableSourceType("image/gif", sdkInt)
            }
        }
    }

    @Test
    fun `원본 형식은 대소문자를 가리지 않는다`() {
        requireUploadableSourceType("IMAGE/HEIC", sdkInt = 28)
        requireUploadableSourceType("Image/Jpeg", sdkInt = 24)
    }

    /** 결과는 늘 JPEG 다. HEIC 를 원본으로 받는다고 발급 검사까지 풀리면 서버 400 을 받는다. */
    @Test
    fun `발급 전 검사는 HEIC 를 여전히 막는다`() {
        assertThrows(ImageUploadException.UnsupportedType::class.java) {
            validateImageUpload("image/heic", fileSize = 1024)
        }
    }

    /** 갤러리 선택기가 이 목록으로 보여 준다. 버전에 따라 HEIC 사진이 보이고 안 보인다. */
    @Test
    fun `원본으로 받는 형식은 28 이상에서만 HEIC 와 HEIF 가 더해진다`() {
        assertEquals(ALLOWED_IMAGE_CONTENT_TYPES, uploadableSourceContentTypes(27))
        assertEquals(
            ALLOWED_IMAGE_CONTENT_TYPES + setOf("image/heic", "image/heif"),
            uploadableSourceContentTypes(28),
        )
    }
}
