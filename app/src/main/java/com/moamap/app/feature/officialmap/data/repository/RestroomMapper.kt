package com.moamap.app.feature.officialmap.data.repository

import com.moamap.app.feature.officialmap.data.remote.RestroomDetailDto
import com.moamap.app.feature.officialmap.data.remote.RestroomListDto
import com.moamap.app.feature.officialmap.data.remote.RestroomMarkerDto
import com.moamap.app.feature.officialmap.domain.model.RestroomDetail
import com.moamap.app.feature.officialmap.domain.model.RestroomMarker
import com.moamap.app.feature.officialmap.domain.model.RestroomMarkers

/** 공공데이터에 이름이 빠진 행이 있다. 마커와 카드가 빈 글자로 뜨지 않게 채운다. */
private const val UNNAMED_RESTROOM = "이름 없는 화장실"

internal fun RestroomListDto.toDomain(): RestroomMarkers =
    RestroomMarkers(restrooms = restrooms.map { it.toDomain() }, truncated = truncated)

internal fun RestroomMarkerDto.toDomain(): RestroomMarker = RestroomMarker(
    id = id,
    name = name.orBlankNull() ?: UNNAMED_RESTROOM,
    latitude = lat,
    longitude = lng,
    category = category.orBlankNull(),
)

internal fun RestroomDetailDto.toDomain(): RestroomDetail = RestroomDetail(
    id = id,
    name = name.orBlankNull() ?: UNNAMED_RESTROOM,
    category = category.orBlankNull(),
    address = roadAddress.orBlankNull() ?: lotAddress.orBlankNull(),
    openHours = openHours.orBlankNull(),
    openHoursDetail = openHoursDetail.orBlankNull(),
    maleToilet = maleToilet ?: 0,
    maleUrinal = maleUrinal ?: 0,
    maleDisabledToilet = maleDisabledToilet ?: 0,
    maleDisabledUrinal = maleDisabledUrinal ?: 0,
    maleChildToilet = maleChildToilet ?: 0,
    maleChildUrinal = maleChildUrinal ?: 0,
    femaleToilet = femaleToilet ?: 0,
    femaleDisabledToilet = femaleDisabledToilet ?: 0,
    femaleChildToilet = femaleChildToilet ?: 0,
    diaperTable = diaperTable == true,
    emergencyBell = emergencyBell == true,
    entranceCctv = entranceCctv == true,
    managerOrg = managerOrg.orBlankNull(),
    phone = phone.orBlankNull(),
    dataRefDate = dataRefDate.orBlankNull(),
)

private fun String?.orBlankNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
