package com.moamap.app.feature.collection.domain.model

/**
 * 방금 만들어진 지도.
 *
 * [inviteCode] 는 프라이빗 지도에만 발급된다. 공개 지도는 null 이다.
 * 따로 발급 API 가 있는 것이 아니라 생성 응답에 함께 실려 온다.
 */
data class CreatedMap(
    val id: Long,
    val inviteCode: String?,
)
