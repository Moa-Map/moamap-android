package com.moamap.app.core.common.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import com.moamap.app.core.network.di.PresignedUploadClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/** 형식을 끝내 알아내지 못했을 때. 갤러리·카메라 모두 보통은 값을 준다. */
private const val DEFAULT_CONTENT_TYPE = "image/jpeg"

private val HEIF_EXTENSION_CONTENT_TYPES = mapOf("heic" to "image/heic", "heif" to "image/heif")

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
    private val compressor: PhotoCompressor,
) : PhotoUploader {

    /**
     * 원본 형식부터 본다. 줄이면 모두 JPEG 가 되므로 서버가 받는 형식이 아니라 줄일 수 있는 형식인지를 본다.
     * HEIC 는 바꿔 올리고, GIF 등은 첫 장면만 조용히 올라가므로 막는다.
     */
    override suspend fun prepare(uri: Uri, maxFileSize: Long): PhotoSpec {
        val sourceType = withContext(Dispatchers.IO) { context.contentResolver.resolveContentType(uri) }
        requireUploadableSourceType(sourceType, sdkInt = Build.VERSION.SDK_INT)
        return compressor.compress(uri, maxFileSize)
    }

    /**
     * 발급받은 주소에 올린다.
     *
     * `Content-Type` 은 발급을 요청할 때 보낸 값과 같아야 한다. 서명에 포함되기 때문에
     * 다르면 스토리지가 거절한다.
     *
     * 본문은 파일에서 곧바로 흘려보낸다. `ByteArray` 로 받아 두면 수 MB 짜리가 통째로 메모리에 올라간다.
     */
    override suspend fun upload(uploadUrl: String, photo: PhotoSpec) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(uploadUrl)
                .put(photo.file.asRequestBody(photo.contentType.toMediaTypeOrNull()))
                .header("Content-Type", photo.contentType)
                .build()

            // 실패는 ErrorInterceptor 가 예외로 바꿔 준다. 성공 응답은 본문이 없어 닫기만 한다.
            client.newCall(request).execute().close()
        }
    }
}

/**
 * 형식을 두 단계로 알아낸다.
 *
 * `getType` 이 null 을 주는 URI 가 있어 확장자로 한 번 더 본다. 그마저 없으면 JPEG 로 둔다 -
 * 실제로 읽을 수 없는 사진이면 줄이는 단계가 막는다.
 *
 * HEIC/HEIF 확장자는 직접 본다. 기기의 MIME 표에 없으면 JPEG 로 잘못 알아, API 28 미만에서 막아야 할
 * 사진이 디코딩 실패(「읽지 못했어요」)로 엉뚱하게 안내된다.
 */
private fun ContentResolver.resolveContentType(uri: Uri): String {
    getType(uri)?.takeIf { it.isNotBlank() }?.let { return it }

    val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        ?.takeIf { it.isNotBlank() }
        ?.lowercase()

    return extension
        ?.let { HEIF_EXTENSION_CONTENT_TYPES[it] ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
        ?.takeIf { it.startsWith("image/") }
        ?: DEFAULT_CONTENT_TYPE
}
