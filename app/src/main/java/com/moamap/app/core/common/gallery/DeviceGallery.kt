package com.moamap.app.core.common.gallery

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DeviceGallery"

/** 기기 갤러리 사진 한 장. 목록에 그릴 만큼만 들고 있는다. */
@Immutable
internal data class DeviceImage(
    val id: Long,
    val uri: Uri,
)

/**
 * 사진을 읽으려면 필요한 권한.
 *
 * 13 부터는 사진만 따로 떼어 준다. 14 부터는 「사진 선택」으로 일부만 허용할 수 있어
 * 그 권한도 함께 요청해야 부분 허용이 선택지로 뜬다. 12 이하는 저장소 권한뿐이다.
 */
internal fun galleryPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )

    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)

    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

/** 사진을 읽을 수 있는 상태. 「사진 선택」으로 일부만 허용한 경우도 읽기는 된다. */
internal fun hasGalleryAccess(context: Context): Boolean =
    galleryPermissions().any { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

/**
 * 14 이상에서 **일부 사진만** 허용한 상태인지.
 *
 * 이때는 허용한 사진만 보이므로 화면이 「사진 더 고르기」를 띄워야 한다. 전체 허용이면 false 다.
 */
internal fun hasPartialGalleryAccess(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false

    val full = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
    val selected = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    return full != PackageManager.PERMISSION_GRANTED && selected == PackageManager.PERMISSION_GRANTED
}

/**
 * 최근 사진을 [limit] 장씩 읽는다. 최신 사진이 앞에 온다.
 *
 * 서버가 받는 형식만 읽는다 - 고르고 나서 "이 형식은 안 돼요" 를 보여주느니 아예 안 보이는 편이 낫다.
 * 읽지 못하면 빈 목록이다. 권한이 없거나 막 회수된 경우라, 화면은 권한 안내를 띄운다.
 */
internal suspend fun loadDeviceImages(
    context: Context,
    mimeTypes: Collection<String>,
    limit: Int,
    offset: Int,
): List<DeviceImage> = withContext(Dispatchers.IO) {
    if (!hasGalleryAccess(context)) return@withContext emptyList()

    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = mimeTypes.joinToString(" OR ") { "${MediaStore.Images.Media.MIME_TYPE} = ?" }
    val selectionArgs = mimeTypes.toTypedArray()
    // LIMIT 은 기기마다 지원이 갈려 정렬만 맡기고 커서를 직접 건너뛴다.
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"

    runCatching {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection.takeIf { mimeTypes.isNotEmpty() },
            selectionArgs.takeIf { mimeTypes.isNotEmpty() },
            sortOrder,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            if (!cursor.moveToPosition(offset)) return@use emptyList()

            buildList {
                do {
                    val id = cursor.getLong(idColumn)
                    add(
                        DeviceImage(
                            id = id,
                            uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id,
                            ),
                        ),
                    )
                } while (size < limit && cursor.moveToNext())
            }
        }.orEmpty()
    }
        .onFailure { throwable -> Log.w(TAG, "갤러리를 읽지 못했습니다", throwable) }
        .getOrDefault(emptyList())
}
