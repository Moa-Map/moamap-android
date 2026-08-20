package com.moamap.app.core.common.upload

/**
 * 서버가 이미지 업로드 주소를 발급해 주는 범위.
 *
 * 지도 커버(`POST /api/v1/maps/cover-upload-url`)와 프로필 이미지
 * (`POST /api/v1/users/profile-upload-url`)의 제약이 같다. 서버가 둘을 다르게 가져가면 그때 나눈다.
 *
 * **한 곳에만 둔다.** 갤러리 선택기가 거르는 형식과 발급 전에 검증하는 형식이 따로 놀면,
 * 서버 계약이 바뀔 때 한쪽만 고쳐도 티가 나지 않는다.
 */
internal val ALLOWED_IMAGE_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")

internal const val MAX_IMAGE_FILE_SIZE = 10L * 1024 * 1024

/**
 * 고른 사진을 올릴 수 없다.
 *
 * 서버 400 을 받고 나서 알리면 "실패했어요" 밖에 보여줄 수 없다. 무엇이 문제인지 알려주려면
 * 보내기 전에 걸러야 한다. presentation 이 메시지를 그대로 스낵바에 쓰므로 public 이다.
 */
sealed class ImageUploadException(message: String) : Exception(message) {

    /** 서버가 발급해 주지 않는 형식이다. */
    class UnsupportedType : ImageUploadException("JPG, PNG, WEBP 형식만 올릴 수 있어요")

    /** 서버 발급 한도를 넘는다. */
    class TooLarge : ImageUploadException("사진 크기는 10MB 이하여야 해요")
}

/**
 * 발급을 요청하기 전에 거른다.
 *
 * `android.net.Uri` 를 받지 않는다. 유닛 테스트에서 `Uri` 를 만들 수 없어서, 판단이 들어간
 * 부분만이라도 밖으로 빼둬야 테스트로 고정된다.
 *
 * 크기를 알아내지 못한 경우(0)는 통과시킨다. `ContentResolver` 가 값을 주지 않는 URI 가 있는데,
 * 모른다는 이유로 막으면 멀쩡한 사진을 못 올린다. 그때는 서버 판단에 맡긴다.
 */
internal fun validateImageUpload(contentType: String, fileSize: Long) {
    if (contentType.lowercase() !in ALLOWED_IMAGE_CONTENT_TYPES) {
        throw ImageUploadException.UnsupportedType()
    }
    if (fileSize > MAX_IMAGE_FILE_SIZE) {
        throw ImageUploadException.TooLarge()
    }
}
