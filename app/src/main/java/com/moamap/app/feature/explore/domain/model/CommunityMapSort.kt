package com.moamap.app.feature.explore.domain.model

/**
 * 커뮤니티 지도 목록 정렬 기준. 순서가 곧 정렬 줄의 순서다.
 */
enum class  CommunityMapSort(val label: String) {
    /**
     * 나에게 맞춘 추천. 목록 조회의 정렬 값이 아니라 추천 API 로 따로 읽는다 -
     * [com.moamap.app.feature.explore.domain.repository.CommunityMapRepository.getCommunityMaps] 에 넘기지 않는다.
     */
    RECOMMENDED("사용자 맞춤"),

    /** 참여 인원 많은 순. */
    POPULAR("인기순"),
    LATEST("최신순"),
}
