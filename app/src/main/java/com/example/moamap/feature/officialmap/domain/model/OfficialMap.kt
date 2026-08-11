package com.example.moamap.feature.officialmap.domain.model

import androidx.compose.runtime.Immutable

/**
 * 공식지도 목록에 그려지는 지도 한 건.
 *
 * 인원·장소 수는 숫자로 들고 표기는 화면에서 만든다. `MyMap` 과 같은 방식이다.
 */
@Immutable
data class OfficialMap(
    val id: Long,
    val title: String,
    val description: String,
    val memberCount: Int,
    val placeCount: Int,
    val joined: Boolean,
)
