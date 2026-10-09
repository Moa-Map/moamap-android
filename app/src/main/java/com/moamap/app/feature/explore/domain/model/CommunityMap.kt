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
    val placeCount: Int,
    val joined: Boolean,
    /** 공식지도. 검색 결과에만 섞여 온다. 누르면 공식지도 화면(유동인구·화장실 등)으로 가야 한다. */
    val official: Boolean = false,
)
