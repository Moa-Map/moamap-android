package com.example.moamap.feature.officialmap.data.repository

import com.example.moamap.feature.officialmap.data.remote.OfficialMapDto
import com.example.moamap.feature.officialmap.domain.model.OfficialMap

/** 이름 없는 지도는 서버 데이터가 깨진 경우다. 카드가 빈 줄로 보이지 않게 자리를 채운다. */
private const val UNTITLED_MAP = "이름 없는 지도"

fun OfficialMapDto.toOfficialMap(): OfficialMap = OfficialMap(
    id = id,
    title = name?.takeIf { it.isNotBlank() } ?: UNTITLED_MAP,
    description = description?.takeIf { it.isNotBlank() }.orEmpty(),
    memberCount = memberCount,
    placeCount = placeCount,
    joined = joined,
)
