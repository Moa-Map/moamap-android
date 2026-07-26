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
    val joined: Boolean = false,
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
    val joined: Boolean = false,
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
