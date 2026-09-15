package com.moamap.app.feature.mapdetail.data.remote

import kotlinx.serialization.Serializable

/**
 * GET api/v1/maps/{mapId}/posts 응답 항목.
 *
 * 작성자는 식별자([userId])만 온다. 닉네임·프로필이 필요한 화면은 따로 조회해야 한다.
 */
@Serializable
data class MapPostDto(
    val id: Long = 0,
    val mapId: Long = 0,
    val userId: Long = 0,
    val content: String? = null,
    val imageUrls: List<String> = emptyList(),
    val placeTags: List<MapPostPlaceTagDto> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** 게시물에 태그된 장소. [name] 은 태그한 시점의 이름이라 장소 이름이 바뀌어도 그대로다. */
@Serializable
data class MapPostPlaceTagDto(
    val placeId: Long = 0,
    val name: String? = null,
)
