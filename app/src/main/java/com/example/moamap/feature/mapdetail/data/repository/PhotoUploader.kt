package com.example.moamap.feature.mapdetail.data.repository

import android.net.Uri

/** 읽어 들인 사진 한 장. presigned URL 발급 요청이 크기와 형식을 요구해 미리 읽는다. */
internal class PhotoBytes(
    val bytes: ByteArray,
    val contentType: String,
) {
    val size: Long get() = bytes.size.toLong()
}

/**
 * 사진을 읽어 presigned URL 로 올린다.
 *
 * 인터페이스로 두는 이유는 [PlaceAddRepositoryImpl] 을 유닛 테스트에서 만들 수 있게
 * 하기 위해서다. 구현체는 `ContentResolver` 와 OkHttp 를 들고 있어 JVM 테스트에서 쓸 수 없다.
 */
internal interface PhotoUploader {

    suspend fun read(uri: Uri): PhotoBytes

    suspend fun upload(uploadUrl: String, photo: PhotoBytes)
}
