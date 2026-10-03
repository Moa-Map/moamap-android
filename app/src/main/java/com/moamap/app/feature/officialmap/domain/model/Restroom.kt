package com.moamap.app.feature.officialmap.domain.model

import androidx.compose.runtime.Immutable

/**
 * 공중화장실 지도의 마커 한 개.
 *
 * 화장실은 지도의 장소(place)가 아니다. 서버가 행안부 공공데이터를 따로 받아 두고 화면 범위로
 * 내려 준다. 장소 목록·장소 상세 API에는 나오지 않는다.
 */
@Immutable
data class RestroomMarker(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** 「공중화장실」「개방화장실」 같은 분류. 없으면 null. */
    val category: String?,
)

/** 화면 범위 조회 결과. */
data class RestroomMarkers(
    val restrooms: List<RestroomMarker>,
    /** 범위 안 화장실이 서버 한도를 넘어 일부만 왔는지. 그러면 화면이 확대를 안내한다. */
    val truncated: Boolean,
)

/** 화장실 한 곳의 자세한 정보. 칸 수는 비어 오면 0이다. */
@Immutable
data class RestroomDetail(
    val id: Long,
    val name: String,
    val category: String?,
    /** 도로명 주소, 없으면 지번 주소. */
    val address: String?,
    val openHours: String?,
    val openHoursDetail: String?,
    val maleToilet: Int,
    val maleUrinal: Int,
    val maleDisabledToilet: Int,
    val maleDisabledUrinal: Int,
    val maleChildToilet: Int,
    val maleChildUrinal: Int,
    val femaleToilet: Int,
    val femaleDisabledToilet: Int,
    val femaleChildToilet: Int,
    val diaperTable: Boolean,
    val emergencyBell: Boolean,
    val entranceCctv: Boolean,
    val managerOrg: String?,
    val phone: String?,
    /** 공공데이터 기준일 `yyyy-MM-dd`. */
    val dataRefDate: String?,
)
