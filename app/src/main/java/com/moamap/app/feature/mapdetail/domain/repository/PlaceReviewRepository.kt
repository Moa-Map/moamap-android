package com.moamap.app.feature.mapdetail.domain.repository

import android.net.Uri
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
     * [photo] 가 있으면 먼저 올린 뒤 작성한다. 형식·크기가 맞지 않으면 발급을 요청하기 전에
     * [com.moamap.app.core.common.upload.ImageUploadException] 을 던진다.
     *
     * 만든 후기를 돌려주지 않는다. 응답에는 작성자 닉네임이 없어 어차피 프로필을 한 번 더
     * 물어야 하고, 그럴 바에는 목록을 다시 읽는 편이 정렬까지 서버가 준 대로 맞는다.
     */
    suspend fun createReview(placeId: Long, content: String, photo: Uri?)

    /**
     * 내 후기의 글을 고친다. 작성자만 할 수 있다.
     *
     * 글만 보낸다. 서버는 보내지 않은 필드를 그대로 두므로 사진은 남는다. 빈 글도 보낸다 -
     * 사진만 남기고 글을 지우는 경우다(비워서 안 보내면 서버가 옛 글을 그대로 둔다).
     */
    suspend fun updateReview(placeId: Long, reviewId: Long, content: String)

    /** 후기를 지운다. 작성자(또는 방장·관리자)만 할 수 있다. */
    suspend fun deleteReview(placeId: Long, reviewId: Long)
}
