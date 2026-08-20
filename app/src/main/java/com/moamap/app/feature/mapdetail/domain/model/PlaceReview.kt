package com.moamap.app.feature.mapdetail.domain.model

/**
 * 장소에 달린 후기 한 건.
 *
 * 서버 응답에는 작성자 식별자만 있어 닉네임은 프로필 조회로 따로 채운다. 이름을 못 얻으면
 * [authorName] 이 null 이고, 화면이 그 자리를 대신 메운다.
 *
 * 별점만 남기고 글은 비워 둘 수 있어 [content] 는 빈 문자열이 될 수 있다.
 */
data class PlaceReview(
    val id: Long,
    val authorId: Long,
    val authorName: String?,
    val rating: Int,
    val content: String,
    /** 작성 시각(epoch millis). 서버 값이 없거나 못 읽으면 null 이다. */
    val createdAtMillis: Long?,
)
