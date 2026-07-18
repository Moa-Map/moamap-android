package com.example.moamap.feature.officialmap.domain.model

/** 서울시 혼잡도 레벨. */
enum class CongestionLevel(val label: String) {
    RELAXED("여유"),
    NORMAL("보통"),
    SLIGHTLY_BUSY("약간 붐빔"),
    BUSY("붐빔"),
    UNKNOWN("정보 없음"),
    ;

    companion object {
        fun fromLabel(label: String?): CongestionLevel =
            entries.firstOrNull { it.label == label } ?: UNKNOWN
    }
}

data class AreaCongestion(
    val level: CongestionLevel,
    val message: String?,
    val populationMin: Long?,
    val populationMax: Long?,
    val femaleRate: Double?,
    val twentiesRate: Double?,
)

/** 유동인구 지역 한 곳. boundary는 GeoJSON Polygon 문자열 그대로 유지한다. */
data class DensityArea(
    val code: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val boundaryGeoJson: String?,
    val congestion: AreaCongestion?,
)
