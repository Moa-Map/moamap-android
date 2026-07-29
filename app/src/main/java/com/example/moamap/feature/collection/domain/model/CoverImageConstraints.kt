package com.example.moamap.feature.collection.domain.model

/**
 * 서버가 커버 업로드 주소를 발급해 주는 범위.
 *
 * `/map-service/v3/api-docs` 의 `POST /api/v1/maps/cover-upload-url` 설명에 적힌 값이다.
 *
 * **한 곳에만 둔다.** 갤러리 선택기가 거르는 형식과 발급 전에 검증하는 형식이 따로 놀면,
 * 서버 계약이 바뀔 때 한쪽만 고쳐도 티가 나지 않는다.
 */
internal val ALLOWED_COVER_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")

internal const val MAX_COVER_FILE_SIZE = 10L * 1024 * 1024
