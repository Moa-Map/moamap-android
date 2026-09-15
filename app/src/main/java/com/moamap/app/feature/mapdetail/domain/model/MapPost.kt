package com.moamap.app.feature.mapdetail.domain.model

import androidx.compose.runtime.Immutable

/** 지도 로그 탭의 게시물 한 건. */
@Immutable
data class MapPost(
    val id: Long,
    val authorId: Long,
    /** 본문. 서버가 비워 보내면 빈 문자열이다. */
    val content: String,
    /** 올린 순서 그대로다. 빈 주소는 뺀다. */
    val imageUrls: List<String>,
    /** 태그된 장소 이름. 올린 순서 그대로이고 빈 이름은 뺀다. */
    val placeNames: List<String>,
    val createdAtMillis: Long?,
)

/** 게시물 목록 정렬. */
enum class MapPostSort {
    /** 최신순. 서버 기본값이다. */
    Latest,

    /** 등록순. */
    Oldest,
}

/** 게시물에 태그할 장소. 이름은 태그하는 시점의 것을 그대로 보낸다. */
@Immutable
data class MapPostPlaceTag(
    val placeId: Long,
    val name: String,
)

/** 새로 올릴 게시물. 사진은 이미 올려 받은 주소다. */
data class NewMapPost(
    val content: String,
    val photoUrls: List<String>,
    val placeTags: List<MapPostPlaceTag>,
)

/** 게시물 목록 한 페이지. */
data class MapPostPage(
    val posts: List<MapPost>,
    /** 더 받을 페이지가 없다. */
    val isLast: Boolean,
)
