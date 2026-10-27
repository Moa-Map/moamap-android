package com.moamap.app.feature.mypage.domain.model

/** 내가 차단한 사용자. 차단 관리 화면 카드 한 장이다. */
data class BlockedUser(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
)
