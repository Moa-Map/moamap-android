package com.moamap.app.feature.collection.data.repository

import com.moamap.app.feature.collection.data.remote.MapSummaryDto
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.mapdetail.domain.model.MapRole

/** 이름 없는 지도는 서버 데이터가 깨진 경우다. 카드가 빈 줄로 보이지 않게 자리를 채운다. */
private const val UNTITLED_MAP = "이름 없는 지도"

fun MapSummaryDto.toMyMap(): MyMap = MyMap(
    id = id,
    title = name?.takeIf { it.isNotBlank() } ?: UNTITLED_MAP,
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    memberCount = memberCount,
    placeCount = placeCount,
    official = type == MapType.Official.requestValue,
    personal = personal,
    role = MapRole.from(myRole),
)
