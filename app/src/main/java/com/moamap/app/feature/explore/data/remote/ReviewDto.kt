package com.moamap.app.feature.explore.data.remote

import kotlinx.serialization.Serializable

/** POST api/v1/places/{placeId}/reviews 요청 */
@Serializable
data class PlaceReviewCreateRequestDto(
    val rating: Int,
    val content: String? = null,
    val imageUrls: List<String>? = null,
)

/** PATCH api/v1/places/{placeId}/reviews/{reviewId} 요청. 변경할 필드만 채운다. */
@Serializable
data class PlaceReviewUpdateRequestDto(
    val rating: Int? = null,
    val content: String? = null,
    val imageUrls: List<String>? = null,
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
