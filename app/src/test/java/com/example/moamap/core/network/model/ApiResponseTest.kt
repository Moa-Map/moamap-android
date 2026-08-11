package com.example.moamap.core.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@Serializable
private data class Payload(val id: Long, val name: String)

class ApiResponseTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `성공 응답에서 data를 꺼낸다`() {
        val body = """{"success":true,"data":{"id":7,"name":"모아맵"},"error":null}"""

        val result = json.decodeFromString<ApiResponse<Payload>>(body)

        assertTrue(result.success)
        assertEquals(7L, result.data?.id)
        assertEquals("모아맵", result.data?.name)
        assertNull(result.error)
    }

    @Test
    fun `본문 없는 성공 응답도 파싱된다`() {
        val body = """{"success":true}"""

        val result = json.decodeFromString<ApiResponse<Payload>>(body)

        assertTrue(result.success)
        assertNull(result.data)
        assertNull(result.error)
    }

    @Test
    fun `실패 응답에서 코드와 상태를 꺼낸다`() {
        val body = """{"success":false,"error":{"code":"COMMON_005","message":"인증이 필요합니다.","status":401}}"""

        val result = json.decodeFromString<ApiResponse<Payload>>(body)

        assertEquals(false, result.success)
        assertEquals("COMMON_005", result.error?.code)
        assertEquals("인증이 필요합니다.", result.error?.message)
        assertEquals(401, result.error?.status)
    }

    @Test
    fun `모르는 필드가 있어도 파싱된다`() {
        val body = """{"success":true,"data":{"id":1,"name":"a","extra":true},"traceId":"abc"}"""

        val result = json.decodeFromString<ApiResponse<Payload>>(body)

        assertEquals(1L, result.data?.id)
    }

    @Test
    fun `error 본문에 일부 필드가 빠져도 기본값으로 채운다`() {
        val body = """{"success":false,"error":{"message":"알 수 없는 오류"}}"""

        val result = json.decodeFromString<ApiResponse<Payload>>(body)

        assertEquals("UNKNOWN", result.error?.code)
        assertEquals(0, result.error?.status)
    }
}
