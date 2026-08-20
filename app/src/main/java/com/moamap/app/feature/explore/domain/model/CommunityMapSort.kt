package com.moamap.app.feature.explore.domain.model

/**
 * 커뮤니티 지도 목록 정렬 기준.
 */
enum class  CommunityMapSort(val label: String) {
    /** 참여 인원 많은 순. */
    POPULAR("인기순"),
    LATEST("최신순"),
}
