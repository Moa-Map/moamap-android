package com.moamap.app.core.common.upload

import android.net.Uri
import java.io.File

/**
 * 올릴 준비가 된 사진 한 장. 줄여 둔 JPEG 임시 파일이다.
 *
 * **바이트를 들고 있지 않는다.** presigned URL 발급이 올릴 파일의 크기와 형식을 미리 요구하는데,
 * 그때 내용까지 메모리에 두면 수 MB 짜리가 그대로 남는다. 장소 사진은 한 번에 다섯 장까지
 * 발급받으므로 다섯 배가 된다. 내용은 업로드하는 순간 [file] 에서 흘려보낸다.
 *
 * [contentType]·[size] 는 줄인 결과의 값이다. presign 서명에 들어가므로 PUT 하는 파일과 반드시 같아야 한다.
 */
internal data class PhotoSpec(
    val file: File,
    val contentType: String,
    val size: Long,
)

internal interface PhotoUploader {

    /**
     * 사진을 줄여 [maxFileSize] 안에 넣는다. 끝까지 넘으면 [ImageUploadException.TooLarge] 다.
     *
     * 만든 임시 파일은 부른 쪽이 지운다. 직접 부르지 말고 [preparing] 으로 감싼다.
     */
    suspend fun prepare(uri: Uri, maxFileSize: Long): PhotoSpec

    /** [PhotoSpec.file] 에서 곧바로 흘려보낸다. 중간에 전부 담아 두지 않는다. */
    suspend fun upload(uploadUrl: String, photo: PhotoSpec)
}

/**
 * 사진을 모두 줄여 둔 뒤 [block] 을 부르고, 끝나면 성공이든 실패든 임시 파일을 지운다.
 *
 * 전부 준비한 뒤에 발급·업로드로 넘어간다. 한 장씩 준비하고 곧바로 올리면, 셋째 사진이 걸렸을 때
 * 앞의 두 장은 이미 올라가 지울 방법이 없다. 중간에 걸리면 그때까지 만든 파일도 지운다.
 */
internal suspend fun <R> PhotoUploader.preparing(
    uris: List<Uri>,
    maxFileSize: Long,
    block: suspend (List<PhotoSpec>) -> R,
): R {
    val prepared = mutableListOf<PhotoSpec>()
    try {
        uris.forEach { uri -> prepared += prepare(uri, maxFileSize) }
        return block(prepared)
    } finally {
        prepared.forEach { photo -> photo.file.delete() }
    }
}

/** 업로드 주소 발급 응답. API 마다 DTO 는 다르지만 쓰는 값은 이 둘이다. */
internal interface IssuedUploadUrl {
    /** 사진을 PUT 할 presigned 주소. */
    val uploadUrl: String

    /** 올린 뒤 서버에 저장할 주소. */
    val fileUrl: String
}

/**
 * 사진 한 장을 줄이기 → 발급 → 업로드 순으로 올리고, 저장할 주소를 돌려준다.
 *
 * 형식·크기는 줄일 때 거른다. 서버 400 을 받고 나서는 이유를 알려줄 수 없다. 발급은 줄인 결과의
 * 형식과 크기로 받는다.
 *
 * 여러 장을 올리는 곳(장소·게시물 사진)은 쓰지 않는다. 전부 준비한 뒤에 올려야, 뒤의 사진이
 * 걸렸을 때 앞의 사진만 올라가 지울 수 없게 남는 일이 없다.
 *
 * @param issue 형식과 크기로 업로드 주소를 발급한다. 지도 커버·프로필·장소 댓글마다 API 가 다르다.
 */
internal suspend fun PhotoUploader.uploadOne(
    uri: Uri,
    maxFileSize: Long = MAX_IMAGE_FILE_SIZE,
    issue: suspend (contentType: String, fileSize: Long) -> IssuedUploadUrl,
): String = preparing(listOf(uri), maxFileSize) { (photo) ->
    val issued = issue(photo.contentType, photo.size)
    require(issued.uploadUrl.isNotBlank() && issued.fileUrl.isNotBlank()) {
        "사진 업로드 주소가 비어 있습니다"
    }
    upload(uploadUrl = issued.uploadUrl, photo = photo)
    issued.fileUrl
}
