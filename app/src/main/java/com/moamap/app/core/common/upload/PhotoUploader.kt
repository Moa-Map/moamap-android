package com.moamap.app.core.common.upload

import android.net.Uri

/**
 * 올릴 사진 한 장의 정보.
 *
 * **바이트를 들고 있지 않는다.** presigned URL 발급이 올릴 파일의 크기와 형식을 미리 요구하는데,
 * 그때 내용까지 읽어 두면 수 MB 짜리가 그대로 메모리에 남는다. 장소 사진은 한 번에 다섯 장까지
 * 발급받으므로 다섯 배가 된다. 내용은 업로드하는 순간 [Uri] 에서 흘려보낸다.
 */
internal data class PhotoSpec(
    val uri: Uri,
    val contentType: String,
    val size: Long,
)

internal interface PhotoUploader {

    /** 내용은 읽지 않고 크기와 형식만 알아낸다. */
    suspend fun inspect(uri: Uri): PhotoSpec

    /** [PhotoSpec.uri] 에서 곧바로 흘려보낸다. 중간에 전부 담아 두지 않는다. */
    suspend fun upload(uploadUrl: String, photo: PhotoSpec)
}

/** 업로드 주소 발급 응답. API 마다 DTO 는 다르지만 쓰는 값은 이 둘이다. */
internal interface IssuedUploadUrl {
    /** 사진을 PUT 할 presigned 주소. */
    val uploadUrl: String

    /** 올린 뒤 서버에 저장할 주소. */
    val fileUrl: String
}

/**
 * 사진 한 장을 살펴보기 → 검증 → 발급 → 업로드 순으로 올리고, 저장할 주소를 돌려준다.
 *
 * 형식·크기는 발급 전에 거른다. 서버 400 을 받고 나서는 이유를 알려줄 수 없다. 내용은 살펴볼 때
 * 읽지 않고 올리는 순간 URI 에서 곧바로 흘려보낸다.
 *
 * 여러 장을 올리는 곳(장소·게시물 사진)은 쓰지 않는다. 전부 검증한 뒤에 올려야, 뒤의 사진이
 * 걸렸을 때 앞의 사진만 올라가 지울 수 없게 남는 일이 없다.
 *
 * @param issue 형식과 크기로 업로드 주소를 발급한다. 지도 커버·프로필·장소 댓글마다 API 가 다르다.
 */
internal suspend fun PhotoUploader.uploadOne(
    uri: Uri,
    maxFileSize: Long = MAX_IMAGE_FILE_SIZE,
    issue: suspend (contentType: String, fileSize: Long) -> IssuedUploadUrl,
): String {
    val photo = inspect(uri)
    validateImageUpload(contentType = photo.contentType, fileSize = photo.size, maxFileSize = maxFileSize)

    val issued = issue(photo.contentType, photo.size)
    require(issued.uploadUrl.isNotBlank() && issued.fileUrl.isNotBlank()) {
        "사진 업로드 주소가 비어 있습니다"
    }
    upload(uploadUrl = issued.uploadUrl, photo = photo)
    return issued.fileUrl
}
