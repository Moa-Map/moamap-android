package com.example.moamap.core.common.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.moamap.core.network.di.PresignedUploadClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** 형식을 끝내 알아내지 못했을 때. 갤러리·카메라 모두 보통은 값을 준다. */
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

    override suspend fun inspect(uri: Uri): PhotoSpec = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        PhotoSpec(
            uri = uri,
            contentType = resolver.resolveContentType(uri),
            size = resolver.resolveSize(uri),
        )
    }

    /**
     * 발급받은 주소에 올린다.
     *
     * `Content-Type` 은 발급을 요청할 때 보낸 값과 같아야 한다. 서명에 포함되기 때문에
     * 다르면 스토리지가 거절한다.
     */
    override suspend fun upload(uploadUrl: String, photo: PhotoSpec) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(uploadUrl)
                .put(UriRequestBody(context.contentResolver, photo))
                .header("Content-Type", photo.contentType)
                .build()

            // 실패는 ErrorInterceptor 가 예외로 바꿔 준다. 성공 응답은 본문이 없어 닫기만 한다.
            client.newCall(request).execute().close()
        }
    }
}

/**
 * 사진을 [Uri] 에서 소켓으로 곧바로 흘려보내는 본문.
 *
 * `ByteArray` 로 받아 두면 수 MB 짜리가 통째로 메모리에 올라간다. 사진 다섯 장이면
 * 저사양 기기에서 터질 수 있다.
 */
private class UriRequestBody(
    private val resolver: ContentResolver,
    private val photo: PhotoSpec,
) : RequestBody() {

    override fun contentType(): MediaType? = photo.contentType.toMediaTypeOrNull()

    /** 알 수 없으면 -1 이고, 그때는 OkHttp 가 청크 전송으로 넘어간다. */
    override fun contentLength(): Long = photo.size.takeIf { it > 0 } ?: -1L

    override fun writeTo(sink: BufferedSink) {
        val stream = resolver.openInputStream(photo.uri)
            ?: throw IOException("사진을 읽지 못했습니다")
        stream.source().use { source -> sink.writeAll(source) }
    }
}

/**
 * 형식을 두 단계로 알아낸다.
 *
 * `getType` 이 null 을 주는 URI 가 있어 확장자로 한 번 더 본다. 그마저 없으면 JPEG 로
 * 둔다 - 이 값이 presign 서명에 들어가므로 업로드할 때와 반드시 같아야 하고, 그래서
 * 여기서 정한 값을 그대로 들고 간다.
 */
private fun ContentResolver.resolveContentType(uri: Uri): String {
    getType(uri)?.takeIf { it.isNotBlank() }?.let { return it }

    val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        ?.takeIf { it.isNotBlank() }
        ?.lowercase()

    return extension
        ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
        ?.takeIf { it.startsWith("image/") }
        ?: DEFAULT_CONTENT_TYPE
}

/** 크기를 모르면 0 을 준다. 발급 요청에는 그대로 싣고, 전송은 청크로 넘어간다. */
private fun ContentResolver.resolveSize(uri: Uri): Long {
    runCatching {
        query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                return cursor.getLong(index)
            }
        }
    }

    return runCatching {
        openAssetFileDescriptor(uri, "r")?.use { descriptor -> descriptor.length }
    }.getOrNull()?.takeIf { it >= 0 } ?: 0L
}
