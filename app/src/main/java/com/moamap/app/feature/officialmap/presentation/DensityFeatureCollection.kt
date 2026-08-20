package com.moamap.app.feature.officialmap.presentation

import com.moamap.app.feature.officialmap.domain.model.CongestionLevel
import com.moamap.app.feature.officialmap.domain.model.DensityArea
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Mapbox GeoJsonSource에 넣을 FeatureCollection 문자열을 만든다.
 * properties: code(지역 코드), level(CongestionLevel.name — 레이어 색상 매칭 키)
 */
internal fun List<DensityArea>.toFeatureCollectionJson(): String {
    val features = buildJsonArray {
        this@toFeatureCollectionJson
            .filter { it.boundaryGeoJson != null }
            .forEach { area ->
                add(
                    buildJsonObject {
                        put("type", "Feature")
                        put("geometry", Json.parseToJsonElement(area.boundaryGeoJson!!))
                        put(
                            "properties",
                            buildJsonObject {
                                put("code", area.code)
                                put("level", (area.congestion?.level ?: CongestionLevel.UNKNOWN).name)
                            },
                        )
                    }
                )
            }
    }
    return JsonObject(
        mapOf(
            "type" to JsonPrimitive("FeatureCollection"),
            "features" to features,
        )
    ).toString()
}
