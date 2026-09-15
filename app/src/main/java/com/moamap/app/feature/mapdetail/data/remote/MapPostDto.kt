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

/**
 * POST api/v1/maps/{mapId}/posts 요청.
 *
 * [placeTags] 는 서버가 검증하지 않고 받은 이름을 그대로 저장한다. 고른 장소의 이름을 보낸다.
 */
@Serializable
data class MapPostCreateRequestDto(
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val placeTags: List<MapPostPlaceTagRequestDto> = emptyList(),
)

@Serializable
data class MapPostPlaceTagRequestDto(
    val placeId: Long,
    val name: String,
)

/** POST api/v1/maps/{mapId}/posts/photo-upload-url 요청. 한 장씩 발급한다. */
@Serializable
data class MapPostPhotoUploadUrlRequestDto(
    val contentType: String,
    val fileSize: Long,
)

/** POST api/v1/maps/{mapId}/posts/photo-upload-url 응답. [fileUrl] 을 게시물 작성 요청에 담는다. */
@Serializable
data class MapPostPhotoUploadUrlDto(
    val uploadUrl: String = "",
    val objectKey: String? = null,
    val fileUrl: String = "",
    val expiresInSeconds: Long = 0,
)
