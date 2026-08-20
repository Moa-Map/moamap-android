package com.moamap.app.feature.officialmap.data.remote

import kotlinx.serialization.Serializable

/**
 * GET api/v1/maps/official 응답 항목.
 *
 * 서버는 `personal` 도 함께 주지만 이 화면이 쓰지 않아 선언하지 않는다.
 * `ignoreUnknownKeys = true` 라 무시된다.
 */
@Serializable
data class OfficialMapDto(
    val id: Long = 0,
    val name: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    // OFFICIAL, COMMUNITY, PRIVATE
    val type: String? = null,
    val tags: List<String> = emptyList(),
    val memberCount: Int = 0,
    val placeCount: Int = 0,
    val joined: Boolean = false,
)
