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

/** 서버가 한 번에 받는 장소 수. 이보다 많으면 나눠 보내야 한다. */
const val MAX_BULK_PLACES = 100

/**
 * POST api/v1/places/bulk 요청.
 *
 * 요청 하나가 지도 하나를 대상으로 한다. 여러 지도에 넣으려면 지도 수만큼 호출한다.
 * 서버가 한 요청당 [MAX_BULK_PLACES] 개로 제한한다.
 */
@Serializable
data class PlaceBulkCreateRequestDto(
    val mapId: Long,
    val places: List<PlaceBulkItemDto>,
)

/** 단건 등록 요청에서 `mapId` 만 뺀 모양. */
@Serializable
data class PlaceBulkItemDto(
    val name: String,
    val address: String? = null,
    val roadAddress: String? = null,
    val lat: Double,
    val lng: Double,
    val category: String? = null,
    val kakaoPlaceId: String,
    // KAKAO_SEARCH, INSTAGRAM, NAVER_MAP, KAKAO_MAP, GOOGLE_MAP
    val sourceType: String,
    val sourceUrl: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
)

/** 일괄 등록 응답. 건별 부분 성공이라 실패한 건도 사유와 함께 온다. */
@Serializable
data class PlaceBulkCreateResponseDto(
    val requested: Int = 0,
    val created: Int = 0,
    val skipped: Int = 0,
    val results: List<PlaceBulkResultDto> = emptyList(),
)

@Serializable
data class PlaceBulkResultDto(
    val index: Int = 0,
    val name: String? = null,
    // CREATED, DUPLICATE, FAILED
    val status: String? = null,
    val placeId: Long? = null,
    val reason: String? = null,
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
    // 기본값을 두지 않는다. 빈 주소가 흘러들어가면 업로드 직전에 알 수 없는 예외로 터진다.
    // 없는 채로 오면 역직렬화 단계에서 바로 걸리는 편이 낫다.
    val uploadUrl: String,
    val fileUrl: String,
    val objectKey: String? = null,
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

/**
 * GET api/v1/places/activities 응답 항목.
 *
 * 서버가 로그 테이블을 따로 두지 않고 `places`·`place_reviews` 의 시각 컬럼에서 이벤트를
 * 역산해 준다. 그래서 **로그 한 건을 가리키는 식별자가 없다** - 목록 키는 화면이 만든다.
 *
 * 후기 작성([type] 이 `REVIEW_CREATED`)은 프라이빗 지도에만 내려온다.
 */
@Serializable
data class PlaceActivityDto(
    // PLACE_ADDED, PLACE_DELETED, REVIEW_CREATED
    val type: String? = null,
    val occurredAt: String? = null,
    val actorId: Long? = null,
    val actorNickname: String? = null,
    val actorProfileImageUrl: String? = null,
    val placeId: Long? = null,
    val placeName: String? = null,
    val reviewId: Long? = null,
    /** 후기 작성 로그의 별점. 다른 종류에는 없다. */
    val rating: Int? = null,
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

/**
 * POST api/v1/places/map-share-extractions 요청.
 *
 * 앱 공유로 들어온 문구가 섞인 텍스트를 그대로 넣어도 서버가 첫 URL 을 뽑아 쓴다.
 */
@Serializable
data class MapShareExtractRequestDto(
    val url: String,
)

/**
 * 지도 공유 링크 추출 응답.
 */
@Serializable
data class MapShareExtractResponseDto(
    // NAVER_MAP, KAKAO_MAP, GOOGLE_MAP
    val source: String? = null,
    val sourceUrl: String? = null,
    val listName: String? = null,
    val owner: String? = null,
    /** 공유 리스트가 스스로 밝힌 장소 수. 없을 수 있다. */
    val declaredCount: Int? = null,
    val extractedCount: Int = 0,
    /** 서버 상한을 넘어 뒤가 잘렸는지. */
    val truncated: Boolean = false,
    val matched: List<MapSharePlaceCandidateDto> = emptyList(),
    val unmatched: List<UnmatchedPlaceDto> = emptyList(),
)

/** 재매칭에 성공한 항목. `mapId` 만 더하면 일괄 등록 요청 항목이 된다. */
@Serializable
data class MapSharePlaceCandidateDto(
    val kakaoPlaceId: String? = null,
    val name: String? = null,
    val category: String? = null,
    val address: String? = null,
    val roadAddress: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val placeUrl: String? = null,
    /** 공유 리스트에 사용자가 적어둔 메모. */
    val description: String? = null,
    val sourceUrl: String? = null,
    // NAVER_MAP, KAKAO_MAP, GOOGLE_MAP
    val sourceType: String? = null,
)

/** 카카오 장소와 매칭하지 못해 등록 후보에서 빠진 장소. */
@Serializable
data class UnmatchedPlaceDto(
    val name: String? = null,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    // NO_RESULT, NAME_MISMATCH, TOO_FAR, SEARCH_FAILED
    val reason: String? = null,
)
