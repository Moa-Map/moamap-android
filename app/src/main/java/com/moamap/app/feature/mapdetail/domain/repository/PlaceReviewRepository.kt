package com.moamap.app.feature.mapdetail.domain.repository

import com.moamap.app.feature.mapdetail.domain.model.PlaceReview

interface PlaceReviewRepository {

    /**
     * 장소에 달린 후기 전부. 최신 글이 앞에 온다.
     *
     * 시트가 한 번에 다 그리는 목록이라 페이지를 나누지 않는다.
     */
    suspend fun getReviews(placeId: Long): List<PlaceReview>

    /**
     * 후기를 남긴다.
     *
     * 지도 멤버만 쓸 수 있다. 아니면 서버가 `PLACE_002` 로 막는다.
     *
     * 만든 후기를 돌려주지 않는다. 응답에는 작성자 닉네임이 없어 어차피 프로필을 한 번 더
     * 물어야 하고, 그럴 바에는 목록을 다시 읽는 편이 정렬까지 서버가 준 대로 맞는다.
     */
    suspend fun createReview(placeId: Long, rating: Int, content: String)
}
