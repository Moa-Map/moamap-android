package com.example.moamap.feature.officialmap.data.repository

import com.example.moamap.feature.officialmap.data.remote.CongestionDto
import com.example.moamap.feature.officialmap.data.remote.FootTrafficAreaDto
import com.example.moamap.feature.officialmap.domain.model.CongestionLevel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FootTrafficMapperTest {

    private val boundary = Json.parseToJsonElement(
        """{"type":"Polygon","coordinates":[[[126.9,37.5],[126.91,37.5],[126.9,37.5]]]}"""
    ).jsonObject

    private val areaDto = FootTrafficAreaDto(
        footTrafficAreaCd = "POI004",
        areaNm = "이태원 관광특구",
        lat = 37.534587,
        lng = 126.995386,
        boundary = boundary,
    )

    @Test
    fun `혼잡도가 있으면 레벨 라벨을 enum으로 변환한다`() {
        val congestion = CongestionDto(
            footTrafficAreaCd = "POI004",
            congestLvl = "약간 붐빔",
            ppltnMin = 88000,
            femaleRate = 57.4,
            ppltnRate20 = 28.9,
        )

        val result = areaDto.toDomain(congestion)

        assertEquals("POI004", result.code)
        assertEquals(CongestionLevel.SLIGHTLY_BUSY, result.congestion?.level)
        assertEquals(88000L, result.congestion?.populationMin)
    }

    @Test
    fun `혼잡도가 없으면 congestion은 null이다`() {
        val result = areaDto.toDomain(congestion = null)

        assertNull(result.congestion)
        assertEquals("이태원 관광특구", result.name)
    }

    @Test
    fun `boundary는 GeoJSON 문자열로 유지된다`() {
        val result = areaDto.toDomain(congestion = null)

        assertEquals(true, result.boundaryGeoJson?.contains("\"type\":\"Polygon\""))
    }

    @Test
    fun `모르는 레벨 라벨은 UNKNOWN으로 변환한다`() {
        assertEquals(CongestionLevel.UNKNOWN, CongestionLevel.fromLabel("대혼잡"))
        assertEquals(CongestionLevel.UNKNOWN, CongestionLevel.fromLabel(null))
        assertEquals(CongestionLevel.RELAXED, CongestionLevel.fromLabel("여유"))
    }
}
