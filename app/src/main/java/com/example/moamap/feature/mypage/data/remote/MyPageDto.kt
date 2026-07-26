package com.example.moamap.feature.mypage.data.remote

import kotlinx.serialization.Serializable

/** PATCH api/v1/users/me 요청. 변경할 필드만 채운다. */
@Serializable
data class UpdateMyPageRequestDto(
    val nickname: String? = null,
    val profileImageUrl: String? = null,
    val email: String? = null,
    val introduction: String? = null,
)

/** GET·PATCH api/v1/users/me 응답 */
@Serializable
data class MyPageDto(
    val id: Long = 0,
    val nickname: String? = null,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val provider: String? = null,
    // USER, ADMIN
    val role: String? = null,
    val lastLoginAt: String? = null,
    val createdAt: String? = null,
    val introduction: String? = null,
)
