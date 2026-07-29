package com.example.moamap.feature.mapdetail.data.repository

import android.net.Uri

/**
 * 올릴 사진 한 장의 정보.
 *
 * **바이트를 들고 있지 않는다.** presigned URL 발급이 사진 다섯 장의 크기와 형식을 한 번에
 * 요구하는데, 그때 내용까지 읽어 두면 수 MB 짜리 다섯 장이 동시에 메모리에 남는다.
 * 내용은 업로드하는 순간 [Uri] 에서 흘려보낸다.
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
