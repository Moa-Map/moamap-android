package com.example.moamap.core.common.upload

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
