package com.moamap.app.feature.officialmap.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** GET map/official/foot-traffic/areas 응답 항목 */
@Serializable
data class FootTrafficAreaDto(
    val footTrafficAreaCd: String,
    val areaNm: String,
    val engNm: String? = null,
    val category: String? = null,
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
    // 연령대별 인구 비율 (0·10·…·70대)
    val ppltnRate0: Double? = null,
    val ppltnRate10: Double? = null,
    val ppltnRate20: Double? = null,
    val ppltnRate30: Double? = null,
    val ppltnRate40: Double? = null,
    val ppltnRate50: Double? = null,
    val ppltnRate60: Double? = null,
    val ppltnRate70: Double? = null,
    // 상주/비상주 인구 비율
    val resntRate: Double? = null,
    val nonResntRate: Double? = null,
    val ppltnTime: String? = null,
    val updatedAt: String? = null,
)
