package com.example.moamap.feature.explore.data.repository

import com.example.moamap.feature.explore.data.remote.CommunityMapDto
import com.example.moamap.feature.explore.data.remote.MapRecommendationDto
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

/**
 * 추천 응답을 목록 카드와 같은 모델로 옮긴다.
 *
 * [CommunityMap.joined] 는 늘 false 다 - 서버가 이미 참여했거나 직접 만든 지도를 추천에서
 * 빼고 준다. 그래서 카드를 누르면 소개 화면으로 가는 것이 맞다.
 *
 * [CommunityMap.placeCount] 는 응답에 없다. 추천 카드는 참여 인원·장소 수 줄을 그리지 않아
 * 화면에 나오지 않는 값이다.
 *
 * [MapRecommendationDto.reason] 은 그릴 자리가 없어 옮기지 않는다.
 */
fun MapRecommendationDto.toDomain(): CommunityMap = CommunityMap(
    id = id,
    title = name?.takeIf { it.isNotBlank() } ?: UNTITLED_MAP,
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    hashtags = tags.filter { it.isNotBlank() },
    memberCount = memberCount,
    placeCount = 0,
    joined = false,
)
