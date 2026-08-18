package com.example.moamap.feature.mapdetail.domain.model

/**
 * 아직 승인되지 않은 장소 등록 요청 한 건.
 *
 * [requesterName] 과 [requesterImageUrl] 은 지금 늘 null 이다. 서버가 승인 대기 응답에
 * 신청자 식별자([requesterId])만 넣어 주고 닉네임·프로필은 아직 내려주지 않는다.
 */
data class PendingPlace(
    /** 장소 식별자. 승인·반려 경로에 그대로 들어간다. */
    val id: Long,
    val placeName: String?,
    val requesterId: Long,
    val requesterName: String?,
    val requesterImageUrl: String?,
    val requestedAtMillis: Long?,
)
