package com.moamap.app.feature.mypage.data.remote

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

/**
 * GET api/v1/users/profiles 응답 항목.
 *
 * 탈퇴했거나 없는 사용자는 응답에서 빠진다. 요청한 id 개수와 항목 수가 다를 수 있다.
 */
@Serializable
data class UserProfileDto(
    val id: Long = 0,
    val nickname: String? = null,
    val profileImageUrl: String? = null,
)

/** POST api/v1/users/profile-upload-url 요청 */
@Serializable
data class ProfileUploadUrlRequestDto(
    val contentType: String,
    val fileSize: Long,
)

/**
 * POST api/v1/users/profile-upload-url 응답.
 *
 * [uploadUrl] 로 직접 PUT 한 뒤 [fileUrl] 을 마이페이지 수정 요청의 `profileImageUrl` 에 담는다.
 */
@Serializable
data class ProfileUploadUrlDto(
    // 기본값을 두지 않는다. 빈 주소가 흘러들어가면 업로드 직전에 알 수 없는 예외로 터진다.
    // 없는 채로 오면 역직렬화 단계에서 바로 걸리는 편이 낫다.
    val uploadUrl: String,
    val fileUrl: String,
    val objectKey: String? = null,
    val expiresInSeconds: Long = 0,
)
