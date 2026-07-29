package com.example.moamap.feature.collection.data.repository

import com.example.moamap.feature.collection.data.remote.MapCreateRequestDto
import com.example.moamap.feature.collection.data.remote.MapDetailDto
import com.example.moamap.feature.collection.domain.model.CreatedMap
import com.example.moamap.feature.collection.domain.model.MapVisibility
import com.example.moamap.feature.collection.domain.model.NewMap

/**
 * 생성 요청.
 *
 * `imageUrl` 은 커버 업로드를 마치고 받은 주소다. 사진을 안 골랐으면 비운다.
 */
fun NewMap.toCreateRequest() = MapCreateRequestDto(
    name = name.trim(),
    description = description.normalized(),
    imageUrl = imageUrl.normalized(),
    visibility = visibility.toRequestValue(),
    tags = tags.normalized(),
)

/**
 * 생성 응답.
 *
 * 초대 코드는 프라이빗 지도에만 발급된다. 공개 지도에는 빈 문자열이 올 수 있어 함께 접는다.
 */
fun MapDetailDto.toCreatedMap() = CreatedMap(
    id = id,
    inviteCode = inviteCode?.takeIf { it.isNotBlank() },
)

private fun MapVisibility.toRequestValue(): String = when (this) {
    MapVisibility.Public -> "PUBLIC"
    MapVisibility.Private -> "PRIVATE"
}

/** 공백뿐인 값은 안 쓴 것과 같다. 빈 문자열이 그대로 저장되지 않게 접는다. */
private fun String?.normalized(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** 빈 목록과 "태그 없음" 을 서버가 다르게 저장하지 않도록 아예 보내지 않는다. */
private fun List<String>.normalized(): List<String>? = takeIf { it.isNotEmpty() }
