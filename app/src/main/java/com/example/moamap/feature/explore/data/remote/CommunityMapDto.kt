package com.example.moamap.feature.explore.data.remote

import kotlinx.serialization.Serializable

/**
 * GET api/v1/maps 응답 항목.
 *
 * 서버의 `MapSummaryResponse` 중 탐색 탭 카드가 그리는 필드만 선언한다.
 * (`ignoreUnknownKeys = true` 라 나머지 필드는 무시된다.)
 * 목록은 COMMUNITY 로 한정돼 내려오므로 `type` 은 받지 않는다.
 */
@Serializable
data class CommunityMapDto(
    val id: Long = 0,
    val name: String? = null,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val memberCount: Int = 0,
    val placeCount: Int = 0,
    val joined: Boolean = false,
)
