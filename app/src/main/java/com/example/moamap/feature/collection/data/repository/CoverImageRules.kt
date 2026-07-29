package com.example.moamap.feature.collection.data.repository

import com.example.moamap.feature.collection.domain.model.CoverImageException

/**
 * 서버가 커버 업로드 주소를 발급해 주는 범위.
 *
 * `/map-service/v3/api-docs` 의 `POST /api/v1/maps/cover-upload-url` 설명에 적힌 값이다.
 */
internal val ALLOWED_COVER_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
internal const val MAX_COVER_FILE_SIZE = 10L * 1024 * 1024

/**
 * 발급을 요청하기 전에 거른다.
 *
 * `android.net.Uri` 를 받지 않는다. 유닛 테스트에서 `Uri` 를 만들 수 없어서, 판단이 들어간
 * 부분만이라도 밖으로 빼둬야 테스트로 고정된다.
 *
 * 크기를 알아내지 못한 경우(0)는 통과시킨다. `ContentResolver` 가 값을 주지 않는 URI 가 있는데,
 * 모른다는 이유로 막으면 멀쩡한 사진을 못 올린다. 그때는 서버 판단에 맡긴다.
 */
internal fun validateCoverImage(contentType: String, fileSize: Long) {
    if (contentType.lowercase() !in ALLOWED_COVER_CONTENT_TYPES) {
        throw CoverImageException.UnsupportedType()
    }
    if (fileSize > MAX_COVER_FILE_SIZE) {
        throw CoverImageException.TooLarge()
    }
}
