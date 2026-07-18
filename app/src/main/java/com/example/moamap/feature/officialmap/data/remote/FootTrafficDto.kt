package com.example.moamap.feature.officialmap.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** GET map/official/foot-traffic/areas 응답 항목 */
@Serializable
data class FootTrafficAreaDto(
    val footTrafficAreaCd: String,
    val areaNm: String,
    val lat: Double,
    val lng: Double,
    // GeoJSON Polygon. 파싱하지 않고 Mapbox 소스에 그대로 전달한다.
    val boundary: JsonObject? = null,
)

/** GET map/official/foot-traffic/congestion 응답 항목 */
@Serializable
data class CongestionDto(
    val footTrafficAreaCd: String,
    val congestLvl: String,
    val congestMsg: String? = null,
    val ppltnMin: Long? = null,
    val ppltnMax: Long? = null,
    val maleRate: Double? = null,
    val femaleRate: Double? = null,
    val ppltnRate20: Double? = null,
)
