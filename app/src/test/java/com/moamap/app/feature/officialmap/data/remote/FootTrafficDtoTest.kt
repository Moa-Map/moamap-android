package com.moamap.app.feature.officialmap.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FootTrafficDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // 로컬 map-service 실제 응답 축약본
    private val areaJson = """
        [{"footTrafficAreaCd":"POI004","areaNm":"이태원 관광특구","engNm":"Itaewon Special Tourist Zone",
          "category":"관광특구","lat":37.534587,"lng":126.995386,
          "boundary":{"type":"Polygon","coordinates":[[[126.991246,37.530305],[126.991207,37.530238],[126.991246,37.530305]]]}}]
    """.trimIndent()

    private val congestionJson = """
        [{"footTrafficAreaCd":"POI004","congestLvl":"여유","congestMsg":"붐빔은 거의 느껴지지 않아요.",
          "ppltnMin":10000,"ppltnMax":12000,"maleRate":47.00,"femaleRate":53.00,
          "ppltnRate0":0.20,"ppltnRate20":28.90,"ppltnTime":"2026-07-18T16:15:00","updatedAt":"2026-07-18T16:45:56"}]
    """.trimIndent()

    @Test
    fun `지역 응답을 역직렬화하면 코드와 경계를 얻는다`() {
        val areas = json.decodeFromString<List<FootTrafficAreaDto>>(areaJson)

        assertEquals(1, areas.size)
        assertEquals("POI004", areas[0].footTrafficAreaCd)
        assertEquals("이태원 관광특구", areas[0].areaNm)
        assertEquals(37.534587, areas[0].lat, 0.000001)
        assertEquals("Polygon", areas[0].boundary?.get("type")?.jsonPrimitive?.content)
    }

    @Test
    fun `혼잡도 응답을 역직렬화하면 레벨과 인구를 얻는다`() {
        val congestions = json.decodeFromString<List<CongestionDto>>(congestionJson)

        assertEquals("여유", congestions[0].congestLvl)
        assertEquals(10000L, congestions[0].ppltnMin)
        assertEquals(28.90, congestions[0].ppltnRate20!!, 0.001)
    }

    @Test
    fun `누락 필드는 null로 채워진다`() {
        val minimal = """[{"footTrafficAreaCd":"X","congestLvl":"보통"}]"""

        val congestions = json.decodeFromString<List<CongestionDto>>(minimal)

        assertNull(congestions[0].ppltnMin)
        assertNull(congestions[0].congestMsg)
    }
}
