package com.moamap.app.feature.officialmap.data.remote

import kotlinx.serialization.Serializable

/** GET maps/official/restrooms 응답. 화면 범위 안 화장실, 최대 500곳. */
@Serializable
data class RestroomListDto(
    val restrooms: List<RestroomMarkerDto> = emptyList(),
    /** 범위 안 화장실이 500곳을 넘어 일부만 왔는지. */
    val truncated: Boolean = false,
)

@Serializable
data class RestroomMarkerDto(
    val id: Long,
    val name: String? = null,
    val lat: Double,
    val lng: Double,
    val category: String? = null,
)

/** GET maps/official/restrooms/{id} 응답. 행안부 공중화장실 공공데이터 한 건. */
@Serializable
data class RestroomDetailDto(
    val id: Long,
    val name: String? = null,
    val category: String? = null,
    val roadAddress: String? = null,
    val lotAddress: String? = null,
    // 칸 수. 남녀 × 일반·장애인·어린이 × 대변기·소변기 (여자 소변기는 없다)
    val maleToilet: Int? = null,
    val maleUrinal: Int? = null,
    val maleDisabledToilet: Int? = null,
    val maleDisabledUrinal: Int? = null,
    val maleChildToilet: Int? = null,
    val maleChildUrinal: Int? = null,
    val femaleToilet: Int? = null,
    val femaleDisabledToilet: Int? = null,
    val femaleChildToilet: Int? = null,
    /** 「정시」「24시간」 같은 구분. 실제 시간은 [openHoursDetail]. */
    val openHours: String? = null,
    val openHoursDetail: String? = null,
    val diaperTable: Boolean? = null,
    val emergencyBell: Boolean? = null,
    val entranceCctv: Boolean? = null,
    val managerOrg: String? = null,
    val phone: String? = null,
    /** 공공데이터 기준일 `yyyy-MM-dd`. */
    val dataRefDate: String? = null,
)
