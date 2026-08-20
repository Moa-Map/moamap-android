package com.moamap.app.feature.officialmap.domain.model

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
    val ageRates: Map<AgeGroup, Double>,
    val maleRate: Double?,
    val femaleRate: Double?,
) {
    /** 비율이 가장 높은 연령대. 동률이면 AgeGroup 선언 순서상 앞선(더 어린) 쪽. */
    val dominantAge: PopulationShare?
        get() = AgeGroup.entries
            .mapNotNull { group -> ageRates[group]?.let { group to it } }
            .maxByOrNull { (_, rate) -> rate }
            ?.let { (group, rate) -> PopulationShare(group.label, rate) }

    /** 비율이 더 높은 성별. 동률이면 여성. */
    val dominantGender: PopulationShare?
        get() {
            val male = maleRate
            val female = femaleRate
            return when {
                male != null && (female == null || male > female) ->
                    PopulationShare(Gender.MALE.label, male)

                female != null -> PopulationShare(Gender.FEMALE.label, female)
                else -> null
            }
        }
}

/** 유동인구 지역 한 곳. boundary는 GeoJSON Polygon 문자열 그대로 유지한다. */
data class DensityArea(
    val code: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val boundaryGeoJson: String?,
    val congestion: AreaCongestion?,
)
