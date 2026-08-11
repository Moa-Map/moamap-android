package com.example.moamap.feature.collection.domain.model

/** 새로 만들 지도. */
data class NewMap(
    val name: String,
    val description: String?,
    val visibility: MapVisibility,
    val tags: List<String>,
    /**
     * 업로드를 마친 커버 이미지 주소.
     *
     * 기기 안의 `content://` 가 아니라 서버가 준 URL 이다. 사진을 안 골랐으면 null.
     */
    val imageUrl: String? = null,
)
