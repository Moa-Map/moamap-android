package com.moamap.app.feature.mypage.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileUploadUrlDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // user-service ProfileUploadUrlResponse. 봉투(ApiResponse)는 컨버터가 벗기므로 안쪽만 온다.
    private val responseJson = """
        {"uploadUrl":"https://storage.example.com/profile/1/abc?X-Amz-Signature=zzz",
         "objectKey":"profile/1/abc.jpg",
         "fileUrl":"https://cdn.example.com/profile/1/abc.jpg",
         "expiresInSeconds":600}
    """.trimIndent()

    @Test
    fun `발급 응답을 역직렬화하면 업로드 주소와 접근 주소를 얻는다`() {
        val issued = json.decodeFromString<ProfileUploadUrlDto>(responseJson)

        assertEquals(
            "https://storage.example.com/profile/1/abc?X-Amz-Signature=zzz",
            issued.uploadUrl,
        )
        assertEquals("https://cdn.example.com/profile/1/abc.jpg", issued.fileUrl)
        assertEquals("profile/1/abc.jpg", issued.objectKey)
        assertEquals(600L, issued.expiresInSeconds)
    }

    /** 서버가 나중에 필드를 더해도 앱이 터지지 않아야 한다. */
    @Test
    fun `모르는 필드가 있어도 역직렬화된다`() {
        val withExtra = """
            {"uploadUrl":"https://up","fileUrl":"https://cdn","someNewField":"값"}
        """.trimIndent()

        val issued = json.decodeFromString<ProfileUploadUrlDto>(withExtra)

        assertEquals("https://up", issued.uploadUrl)
        assertEquals("https://cdn", issued.fileUrl)
    }

    @Test
    fun `발급 요청은 형식과 크기를 담는다`() {
        val encoded = json.encodeToString(
            ProfileUploadUrlRequestDto(contentType = "image/jpeg", fileSize = 2048),
        )

        assertEquals("""{"contentType":"image/jpeg","fileSize":2048}""", encoded)
    }
}
