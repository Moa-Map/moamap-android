package com.example.moamap.feature.collection.domain.model

import androidx.compose.runtime.Immutable

/**
 * 모음 화면 목록에 그려지는 내 지도 한 건.
 *
 * 등록 장소 수는 담지 않는다. 서버 목록 응답(`MapSummaryResponse`)에 해당 필드가 없다.
 * 필드가 추가되면 그때 함께 넣는다.
 */
@Immutable
data class MyMap(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val memberCount: Int,
    /** 공식 인증 배지. 이 화면은 커뮤니티·프라이빗만 조회하므로 지금은 항상 false 다. */
    val official: Boolean,
)
