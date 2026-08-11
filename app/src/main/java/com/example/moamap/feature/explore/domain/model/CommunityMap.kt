package com.example.moamap.feature.explore.domain.model

import androidx.compose.runtime.Immutable

/** 탐색 탭 목록에 그려지는 커뮤니티 지도 한 건. */
@Immutable
data class CommunityMap(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val hashtags: List<String>,
    val memberCount: Int,
    val placeCount: Int,
    val joined: Boolean,
)
