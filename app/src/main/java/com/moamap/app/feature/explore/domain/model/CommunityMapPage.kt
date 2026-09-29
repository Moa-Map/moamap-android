package com.moamap.app.feature.explore.domain.model

/** 커뮤니티 지도 목록 한 페이지. [isLast] 면 더 받을 페이지가 없다. */
data class CommunityMapPage(
    val maps: List<CommunityMap>,
    val isLast: Boolean,
)
