package com.moamap.app.feature.explore.domain.model

import androidx.compose.runtime.Immutable

/** 탐색 탭 목록에 그려지는 커뮤니티 지도 한 건. */
@Immutable
data class CommunityMap(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val hashtags: List<String>,
    val memberCount: Int,
    /** 등록된 장소 수. 모르면 null - 추천 응답에는 이 값이 없어 카드가 장소 수를 숨긴다. */
    val placeCount: Int?,
    val joined: Boolean,
)
