package com.moamap.app.feature.mapdetail.domain.model

/**
 * 아직 승인되지 않은 장소 등록 요청 한 건.
 *
 * [requesterName] 과 [requesterImageUrl] 은 서버가 신청자 프로필을 못 찾으면 null 이다.
 */
data class PendingPlace(
    /** 장소 식별자. 승인·반려 경로에 그대로 들어간다. */
    val id: Long,
    val placeName: String?,
    val requesterName: String?,
    val requesterImageUrl: String?,
    val requestedAtMillis: Long?,
)
