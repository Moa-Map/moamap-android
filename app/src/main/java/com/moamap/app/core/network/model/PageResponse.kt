package com.moamap.app.core.network.model

import kotlinx.serialization.Serializable

/**
 * 서버 페이지네이션 공통 응답. envelope 안쪽 페이로드로 내려온다.
 *
 * 각 feature 는 이 타입을 페이로드로 선언한다.
 * 예) suspend fun getMaps(): PageResponse<MapSummaryDto>
 */
@Serializable
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val last: Boolean = true,
)
