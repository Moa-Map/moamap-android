package com.example.moamap.feature.explore.data.remote

import kotlinx.serialization.Serializable

/** POST api/v1/places 요청 */
@Serializable
data class PlaceCreateRequestDto(
    val name: String,
    val address: String? = null,
    val roadAddress: String? = null,
    val lat: Double,
    val lng: Double,
    val category: String? = null,
    val kakaoPlaceId: String,
    // KAKAO_SEARCH, INSTAGRAM
    val sourceType: String,
    val sourceUrl: String? = null,
    val description: String? = null,
    val mapId: Long,
    val tags: List<String>? = null,
    /** 서버가 최대 5장으로 제한한다. presigned URL 로 올린 뒤의 접근 주소다. */
    val photoUrls: List<String>? = null,
)

/** PATCH api/v1/places/{id} 요청. 변경할 필드만 채운다. */
@Serializable
data class PlaceUpdateRequestDto(
    val name: String? = null,
    val address: String? = null,
    val roadAddress: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val category: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
)

/**
 * POST api/v1/places/photo-upload-url 요청.
 *
 * 발급 권한은 장소 등록 권한과 같아 [mapId] 가 필요하다. 한 번에 최대 5장.
 */
@Serializable
data class PhotoUploadUrlRequestDto(
    val mapId: Long,
    val files: List<PhotoFileSpecDto>,
)

@Serializable
data class PhotoFileSpecDto(
    val contentType: String,
    val fileSize: Long,
)

/**
 * POST api/v1/places/photo-upload-url 응답 항목.
 *
 * [uploadUrl] 로 직접 PUT 한 뒤 [fileUrl] 을 장소 등록 요청의 `photoUrls` 에 담는다.
 */
@Serializable
data class PhotoUploadUrlDto(
    val uploadUrl: String = "",
    val objectKey: String? = null,
    val fileUrl: String = "",
    val expiresInSeconds: Long = 0,
)

/** POST api/v1/places/instagram-extractions 요청 */
@Serializable
data class InstagramExtractRequestDto(
    val url: String,
    val description: String,
)

/** 장소 조회/등록/수정 응답 */
@Serializable
data class PlaceDto(
    val id: Long = 0,
    val name: String? = null,
    val address: String? = null,
    val roadAddress: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val category: String? = null,
    val kakaoPlaceId: String? = null,
    // KAKAO_SEARCH, INSTAGRAM
    val sourceType: String? = null,
    val sourceUrl: String? = null,
    val description: String? = null,
    val mapId: Long = 0,
    val createdBy: Long = 0,
    // PENDING, APPROVED, REJECTED
    val status: String? = null,
    val avgRating: Double? = null,
    val commentCount: Int = 0,
    val processedBy: Long? = null,
    val processedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val tags: List<String> = emptyList(),
    val photoUrls: List<String> = emptyList(),
)

/** POST api/v1/places/instagram-extractions 응답 항목 (장소 후보) */
@Serializable
data class PlaceCandidateDto(
    val kakaoPlaceId: String? = null,
    val name: String? = null,
    val category: String? = null,
    val address: String? = null,
    val roadAddress: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val placeUrl: String? = null,
    val sourceUrl: String? = null,
)
