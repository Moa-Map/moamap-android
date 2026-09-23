package com.moamap.app.feature.explore.data.remote

import kotlinx.serialization.Serializable

/** POST api/v1/places/{placeId}/comments 요청 */
@Serializable
data class PlaceReviewCreateRequestDto(
    val rating: Int,
    val content: String? = null,
    val imageUrls: List<String>? = null,
)

/** PATCH api/v1/places/{placeId}/comments/{commentId} 요청. 변경할 필드만 채운다. */
@Serializable
data class PlaceReviewUpdateRequestDto(
    val rating: Int? = null,
    val content: String? = null,
    val imageUrls: List<String>? = null,
)

/** POST api/v1/places/{placeId}/comments/photo-upload-url 요청. 서버가 한 장만 받는다. */
@Serializable
data class PlaceReviewPhotoUploadUrlRequestDto(
    val contentType: String,
    val fileSize: Long,
)

/** POST api/v1/places/{placeId}/comments/photo-upload-url 응답 */
@Serializable
data class PlaceReviewPhotoUploadUrlDto(
    // 기본값을 두지 않는다. 빈 주소로 업로드를 시도하기 전에 역직렬화에서 걸리게 한다.
    val uploadUrl: String,
    val fileUrl: String,
    val objectKey: String? = null,
    val expiresInSeconds: Long = 0,
)

/** 리뷰 조회/작성/수정 응답 */
@Serializable
data class PlaceReviewDto(
    val id: Long = 0,
    val placeId: Long = 0,
    val userId: Long = 0,
    val rating: Int = 0,
    val content: String? = null,
    val imageUrls: List<String> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
