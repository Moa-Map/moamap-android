package com.example.moamap.feature.collection.domain.model

/**
 * 새로 만들 지도.
 *
 * 커버 이미지는 담지 않는다. 서버는 URL 만 받는데 사용자가 고른 사진은 아직 기기 안에만 있고,
 * 업로드에 필요한 `mapId` 는 지도를 만들어야 나온다.
 */
data class NewMap(
    val name: String,
    val description: String?,
    val visibility: MapVisibility,
    val tags: List<String>,
)
