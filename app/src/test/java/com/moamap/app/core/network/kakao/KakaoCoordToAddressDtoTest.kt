package com.moamap.app.core.network.kakao

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KakaoCoordToAddressDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `도로명과 지번이 모두 오면 둘 다 읽는다`() {
        val response = """
            {
              "meta": { "total_count": 1 },
              "documents": [
                {
                  "road_address": {
                    "address_name": "서울 동작구 상도로 369",
                    "building_name": "숭실대학교"
                  },
                  "address": {
                    "address_name": "서울 동작구 상도동 511"
                  }
                }
              ]
            }
        """.trimIndent()

        val decoded = json.decodeFromString(KakaoCoordToAddressDto.serializer(), response)

        val document = decoded.documents.single()
        assertEquals("서울 동작구 상도로 369", document.roadAddress?.addressName)
        assertEquals("서울 동작구 상도동 511", document.address?.addressName)
    }

    @Test
    fun `도로명이 없는 곳은 null 로 온다`() {
        // 산이나 논밭에는 도로명 주소가 없다. 이때 지번으로 대체해야 한다.
        val response = """
            {
              "documents": [
                { "road_address": null, "address": { "address_name": "서울 동작구 상도동 산65" } }
              ]
            }
        """.trimIndent()

        val decoded = json.decodeFromString(KakaoCoordToAddressDto.serializer(), response)

        assertNull(decoded.documents.single().roadAddress)
        assertEquals("서울 동작구 상도동 산65", decoded.documents.single().address?.addressName)
    }

    @Test
    fun `바다 한가운데면 결과가 비어 있다`() {
        val decoded =
            json.decodeFromString(KakaoCoordToAddressDto.serializer(), """{"documents":[]}""")

        assertEquals(emptyList<KakaoCoordAddressDocumentDto>(), decoded.documents)
    }
}
