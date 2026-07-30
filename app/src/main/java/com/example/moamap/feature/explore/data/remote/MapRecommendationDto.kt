package com.example.moamap.feature.explore.data.remote

import kotlinx.serialization.Serializable

/**
 * GET api/v1/maps/recommendations 응답 항목.
 *
 * 서버의 `MapRecommendationResponse` 중 추천 카드가 그리는 필드만 선언한다.
 * (`ignoreUnknownKeys = true` 라 `description` 과 `type` 은 무시된다.)
 *
 * 목록 응답과 달리 `joined` 와 `placeCount` 가 없다 - 서버가 이미 참여했거나 직접 만든
 * 지도를 추천에서 빼고 주므로 참여 여부를 실어 보낼 이유가 없다.
 */
@Serializable
data class MapRecommendationDto(
    val id: Long = 0,
    val name: String? = null,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val memberCount: Int = 0,
    /**
     * 추천 근거. `"관심 태그 #카페 #데이트와 비슷해요"` 또는 `"지금 많이 찾는 지도예요"`.
     *
     * 아직 카드에 그릴 자리가 없어 도메인 모델로 옮기지 않는다. 자리가 잡히면 매퍼에서
     * 넘기면 된다.
     */
    val reason: String? = null,
)
