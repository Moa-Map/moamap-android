package com.example.moamap.feature.mapdetail.data.repository

import android.content.Context
import android.net.Uri
import com.example.moamap.core.network.di.PresignedUploadClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** 스토리지가 알아보지 못하는 형식이 오면 이걸로 올린다. */
private const val DEFAULT_CONTENT_TYPE = "image/jpeg"

/**
 * presigned URL 로 사진을 올린다.
 *
 * 인증 헤더가 붙지 않은 클라이언트를 쓴다. presigned URL 은 주소에 서명이 들어 있어
 * `Authorization` 이 함께 가면 스토리지가 인증 방식이 겹쳤다고 보고 거절한다.
 */
@Singleton
internal class PresignedPhotoUploader @Inject constructor(
    @PresignedUploadClient private val client: OkHttpClient,
    @ApplicationContext private val context: Context,
) : PhotoUploader {

    override suspend fun read(uri: Uri): PhotoBytes = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
            ?: throw IOException("사진을 읽지 못했습니다")

        PhotoBytes(
            bytes = bytes,
            contentType = resolver.getType(uri) ?: DEFAULT_CONTENT_TYPE,
        )
    }

    /**
     * 발급받은 주소에 올린다.
     *
     * `Content-Type` 은 발급을 요청할 때 보낸 값과 같아야 한다. 서명에 포함되기 때문에
     * 다르면 스토리지가 거절한다.
     */
    override suspend fun upload(uploadUrl: String, photo: PhotoBytes) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(uploadUrl)
                .put(photo.bytes.toRequestBody(photo.contentType.toMediaTypeOrNull()))
                .header("Content-Type", photo.contentType)
                .build()

            // 실패는 ErrorInterceptor 가 예외로 바꿔 준다. 성공 응답은 본문이 없어 닫기만 한다.
            client.newCall(request).execute().close()
        }
    }
}
