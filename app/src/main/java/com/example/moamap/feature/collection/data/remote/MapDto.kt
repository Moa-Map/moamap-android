package com.example.moamap.feature.collection.data.remote

import kotlinx.serialization.Serializable

/** POST api/v1/maps 요청 */
@Serializable
data class MapCreateRequestDto(
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    // PUBLIC, PRIVATE
    val visibility: String,
    val tags: List<String>? = null,
)

/** PATCH api/v1/maps/{mapId} 요청 */
@Serializable
data class MapUpdateRequestDto(
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val tags: List<String>? = null,
)

/**
 * POST api/v1/maps/cover-upload-url 요청.
 *
 * 지도를 만들기 전 커버를 고르는 화면에서 쓰므로 `mapId` 가 없다.
 */
@Serializable
data class CoverUploadUrlRequestDto(
    val contentType: String,
    val fileSize: Long,
)

/**
 * POST api/v1/maps/cover-upload-url 응답.
 *
 * [uploadUrl] 로 직접 PUT 한 뒤 [fileUrl] 을 지도 생성 요청의 `imageUrl` 에 담는다.
 */
@Serializable
data class CoverUploadUrlDto(
    // 기본값을 두지 않는다. 빈 주소가 흘러들어가면 업로드 직전에 알 수 없는 예외로 터진다.
    // 없는 채로 오면 역직렬화 단계에서 바로 걸리는 편이 낫다.
    val uploadUrl: String,
    val fileUrl: String,
    val objectKey: String? = null,
    val expiresInSeconds: Long = 0,
)

/** POST api/v1/maps/join 요청 */
@Serializable
data class JoinByInviteCodeRequestDto(
    val inviteCode: String,
)

/** GET api/v1/maps, /me 응답 항목 */
@Serializable
data class MapSummaryDto(
    val id: Long = 0,
    val name: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    // OFFICIAL, COMMUNITY, PRIVATE
    val type: String? = null,
    val tags: List<String> = emptyList(),
    val memberCount: Int = 0,
    val placeCount: Int = 0,
    val joined: Boolean = false,
    /** 로그인할 때 기본으로 생기는 개인 지도. 프라이빗 탭에서 따로 묶는다. */
    val personal: Boolean = false,
)

/** 지도 상세/생성/수정/합류 응답 */
@Serializable
data class MapDetailDto(
    val id: Long = 0,
    val name: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    // OFFICIAL, COMMUNITY, PRIVATE
    val type: String? = null,
    val ownerId: Long = 0,
    val tags: List<String> = emptyList(),
    val memberCount: Int = 0,
    val placeCount: Int = 0,
    val joined: Boolean = false,
    /** 가입할 때 자동으로 생기는 "나만의 지도". PRIVATE 로 내려와 type 으로는 못 가린다. */
    val personal: Boolean = false,
    // OWNER, ADMIN, MEMBER, NONE
    val myRole: String? = null,
    val inviteCode: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** GET api/v1/maps/{mapId}/members/{userId} 응답 */
@Serializable
data class MapMemberRoleDto(
    // OFFICIAL, COMMUNITY, PRIVATE
    val mapType: String? = null,
    // OWNER, ADMIN, MEMBER, NONE
    val role: String? = null,
)

/** GET api/v1/maps/{mapId}/members 응답 */
@Serializable
data class MapMemberListDto(
    val memberCount: Int = 0,
    val members: List<MapMemberSummaryDto> = emptyList(),
)

/** [MapMemberListDto] 의 한 사람. 등록한 장소 수는 아직 서버가 내려주지 않는다. */
@Serializable
data class MapMemberSummaryDto(
    val userId: Long = 0,
    val nickname: String? = null,
    val profileImageUrl: String? = null,
    // OWNER, ADMIN, MEMBER, NONE
    val role: String? = null,
)

/** PUT api/v1/maps/{mapId}/members/{userId}/role 요청 */
@Serializable
data class MapMemberRoleUpdateRequestDto(
    // OWNER, ADMIN, MEMBER, NONE
    val role: String,
)

/** PUT api/v1/maps/{mapId}/members/{userId}/role 응답 */
@Serializable
data class MapMemberRoleUpdateDto(
    val mapId: Long = 0,
    val userId: Long = 0,
    // OWNER, ADMIN, MEMBER, NONE
    val role: String? = null,
)
