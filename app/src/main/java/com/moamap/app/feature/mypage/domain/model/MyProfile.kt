package com.moamap.app.feature.mypage.domain.model

/**
 * 프로필 편집 화면이 쓰는 내 정보.
 *
 * 응답의 `provider`·`role`·`lastLoginAt`·`createdAt` 은 아직 어느 화면도 쓰지 않아 담지 않는다.
 */
data class MyProfile(
    val id: Long,
    val nickname: String,
    val email: String,
    val profileImageUrl: String?,
    val introduction: String,
)
