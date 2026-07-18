package com.example.moamap.feature.officialmap.presentation

import com.example.moamap.feature.officialmap.domain.model.AreaCongestion
import com.example.moamap.feature.officialmap.domain.model.CongestionLevel
import com.example.moamap.feature.officialmap.domain.model.DensityArea
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class DensityFeatureCollectionTest {

    private val polygon = """{"type":"Polygon","coordinates":[[[126.9,37.5],[126.91,37.5],[126.9,37.5]]]}"""

    private fun area(code: String, level: CongestionLevel?, boundary: String? = polygon) = DensityArea(
        code = code, name = "지역$code", lat = 37.5, lng = 126.9,
        boundaryGeoJson = boundary,
        congestion = level?.let {
            AreaCongestion(it, null, null, null, null, null)
        },
    )

    @Test
    fun `지역들을 FeatureCollection으로 변환한다`() {
        val json = listOf(area("A", CongestionLevel.BUSY)).toFeatureCollectionJson()
        val root = Json.parseToJsonElement(json).jsonObject

        assertEquals("FeatureCollection", root["type"]?.jsonPrimitive?.content)
        val feature = root["features"]!!.jsonArray[0].jsonObject
        assertEquals("A", feature["properties"]!!.jsonObject["code"]?.jsonPrimitive?.content)
        assertEquals("BUSY", feature["properties"]!!.jsonObject["level"]?.jsonPrimitive?.content)
        assertEquals("Polygon", feature["geometry"]!!.jsonObject["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `혼잡도 없는 지역은 UNKNOWN 레벨이 된다`() {
        val json = listOf(area("A", level = null)).toFeatureCollectionJson()
        val feature = Json.parseToJsonElement(json).jsonObject["features"]!!.jsonArray[0].jsonObject

        assertEquals("UNKNOWN", feature["properties"]!!.jsonObject["level"]?.jsonPrimitive?.content)
    }

    @Test
    fun `boundary 없는 지역은 제외한다`() {
        val json = listOf(
            area("A", CongestionLevel.RELAXED),
            area("B", CongestionLevel.BUSY, boundary = null),
        ).toFeatureCollectionJson()

        assertEquals(1, Json.parseToJsonElement(json).jsonObject["features"]!!.jsonArray.size)
    }
}
