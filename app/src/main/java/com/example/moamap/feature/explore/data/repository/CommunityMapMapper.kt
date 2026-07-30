package com.example.moamap.feature.explore.data.repository

import com.example.moamap.feature.explore.data.remote.CommunityMapDto
import com.example.moamap.feature.explore.domain.model.CommunityMap

/** 이름 없는 지도는 서버 데이터가 깨진 경우다. 카드가 빈 줄로 보이지 않게 자리를 채운다. */
private const val UNTITLED_MAP = "이름 없는 지도"

fun CommunityMapDto.toDomain(): CommunityMap = CommunityMap(
    id = id,
    title = name?.takeIf { it.isNotBlank() } ?: UNTITLED_MAP,
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    hashtags = tags.filter { it.isNotBlank() },
    memberCount = memberCount,
    placeCount = placeCount,
    joined = joined,
)
