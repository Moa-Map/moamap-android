package com.example.moamap.feature.collection.data.repository

import com.example.moamap.feature.collection.data.remote.MapCreateRequestDto
import com.example.moamap.feature.collection.domain.model.MapVisibility
import com.example.moamap.feature.collection.domain.model.NewMap

/**
 * 생성 요청.
 *
 * `imageUrl` 은 항상 비운다. 서버는 URL 문자열만 받는데 사용자가 고른 사진을 URL 로
 * 바꿔줄 업로드 엔드포인트가 아직 없다.
 */
fun NewMap.toCreateRequest() = MapCreateRequestDto(
    name = name.trim(),
    description = description.normalized(),
    imageUrl = null,
    visibility = visibility.toRequestValue(),
    tags = tags.normalized(),
)

private fun MapVisibility.toRequestValue(): String = when (this) {
    MapVisibility.Public -> "PUBLIC"
    MapVisibility.Private -> "PRIVATE"
}

/** 공백뿐인 설명은 안 쓴 것과 같다. */
private fun String?.normalized(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** 빈 목록과 "태그 없음" 을 서버가 다르게 저장하지 않도록 아예 보내지 않는다. */
private fun List<String>.normalized(): List<String>? = takeIf { it.isNotEmpty() }
