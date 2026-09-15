package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.mapdetail.data.remote.MapPostDto
import com.moamap.app.feature.mapdetail.domain.model.MapPost

/**
 * 게시물 응답을 도메인으로 옮긴다.
 *
 * 빈 사진 주소와 빈 장소 이름은 여기서 뺀다. 남겨 두면 카드가 첫 사진 자리에 빈 이미지를,
 * 장소 자리에 빈 알약을 그린다.
 */
fun MapPostDto.toMapPost(): MapPost = MapPost(
    id = id,
    authorId = userId,
    content = content.orEmpty(),
    imageUrls = imageUrls.map { url -> url.trim() }.filter { url -> url.isNotEmpty() },
    placeNames = placeTags.mapNotNull { tag -> tag.name?.trim()?.takeIf { name -> name.isNotEmpty() } },
    createdAtMillis = parseServerDateTime(createdAt),
)
