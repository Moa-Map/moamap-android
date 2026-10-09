package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.mapdetail.data.remote.MapPostCommentDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostDto
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import com.moamap.app.feature.mapdetail.domain.model.MapPostComment

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

/** 게시물 댓글. 작성자 이름·사진은 프로필 조회로 받은 것을 넘긴다. 빈 값은 null 로 둔다. */
fun MapPostCommentDto.toMapPostComment(authorName: String?, authorImageUrl: String?): MapPostComment = MapPostComment(
    id = id,
    authorId = userId,
    authorName = authorName?.trim()?.takeIf { name -> name.isNotEmpty() },
    authorImageUrl = authorImageUrl?.trim()?.takeIf { url -> url.isNotEmpty() },
    content = content?.trim().orEmpty(),
    createdAtMillis = parseServerDateTime(createdAt),
)
