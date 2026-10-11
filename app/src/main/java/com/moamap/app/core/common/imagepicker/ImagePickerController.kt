package com.moamap.app.core.common.imagepicker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.moamap.app.core.common.gallery.GalleryPickerDialog
import java.io.File

/** 촬영본 확장자. 카메라 앱은 JPEG 로 저장한다. */
private const val DEFAULT_PHOTO_EXTENSION = "jpg"

/** 카메라·갤러리 실행 진입점. 권한 확인은 안에서 끝내고 호출부는 어느 쪽인지만 고른다. */
@Stable
internal class ImagePickerController(
    private val onRequestCamera: () -> Unit,
    private val onRequestGallery: () -> Unit,
) {
    fun requestCamera() = onRequestCamera()

    fun requestGallery() = onRequestGallery()
}

/**
 * 카메라 런처·권한 요청과 앱 안 갤러리를 묶어 [ImagePickerController] 로 돌려준다.
 *
 * 촬영본은 [cacheDirectoryName] 아래에 만들어 [FileProvider] 로 넘긴다. 새 디렉터리를 쓰려면
 * `res/xml/profile_image_paths.xml` 에 `cache-path` 를 함께 추가해야 한다.
 *
 * 갤러리는 시스템 사진 선택기가 아니라 앱 안 갤러리를 화면 전체를 덮는 창으로 띄운다(10-11 사용자
 * 결정 - 모든 사진 고르기를 로그 게시물과 같은 화면으로). 창이라 장소 시트 안이나 팝업 메뉴에서 열어도
 * 맨 위에 뜨고, 기기 뒤로가기는 갤러리부터 닫는다. 사진 접근 권한은 갤러리가 열릴 때 묻는다. 고른
 * 사진은 기기 사진 주소 그대로 넘긴다 - 권한이 있는 동안은 앱이 다시 살아나도 읽힌다.
 *
 * 갤러리에서 여러 장을 고르면 [onImageSelected] 를 고른 순서대로 한 장씩 부른다.
 *
 * 컨트롤러는 recomposition 마다 새로 만든다 - 붙잡고 있는 런처들은 이미 remember 된 값이라
 * 새로 만들어도 같은 대상을 가리키고, 클릭 시점에 최신 [onImageSelected] 를 보게 하려면
 * 오히려 붙잡아두지 않는 편이 안전하다.
 *
 * @param galleryTitle 갤러리 맨 위 제목. 고르는 사진 이름이다(「지도 사진」 등).
 * @param maxSelectable 갤러리에서 한 번에 고를 수 있는 장수.
 */
@Composable
internal fun rememberImagePickerController(
    state: ImagePickerState,
    cacheDirectoryName: String,
    fileNamePrefix: String,
    galleryTitle: String,
    maxSelectable: Int = 1,
    onImageSelected: (Uri) -> Unit = {},
): ImagePickerController {
    val context = LocalContext.current
    val currentOnImageSelected by rememberUpdatedState(onImageSelected)
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        pendingCameraUri?.let(Uri::parse)?.let { uri ->
            if (success) {
                state.dismissSourceMenu()
                currentOnImageSelected(uri)
            } else {
                // 촬영을 취소하면 만들어둔 빈 파일이 캐시에 남는다.
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }
        pendingCameraUri = null
    }

    val launchCamera: () -> Unit = {
        createImageCaptureUri(context, cacheDirectoryName, fileNamePrefix)?.let { uri ->
            pendingCameraUri = uri.toString()
            cameraLauncher.launch(uri)
        }
        Unit
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) launchCamera()
    }

    val requestCamera: () -> Unit = {
        state.dismissSourceMenu()
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (state.isGalleryVisible) {
        GalleryPickerDialog(
            title = galleryTitle,
            maxSelectable = maxSelectable,
            // 갤러리 첫 칸의 카메라. 갤러리를 닫고 메뉴의 카메라와 같은 길로 간다.
            onCameraClick = {
                state.dismissGallery()
                requestCamera()
            },
            onConfirm = { uris ->
                state.dismissGallery()
                uris.forEach { uri -> currentOnImageSelected(uri) }
            },
            onDismiss = state::dismissGallery,
        )
    }

    return ImagePickerController(
        onRequestCamera = requestCamera,
        onRequestGallery = state::showGallery,
    )
}

/** 캐시 폴더에 빈 파일을 만든다. 폴더는 `res/xml/profile_image_paths.xml` 에 등록돼 있어야 한다. */
private fun createCacheFile(
    context: Context,
    cacheDirectoryName: String,
    fileNamePrefix: String,
    extension: String,
): File {
    val imageDirectory = File(context.cacheDir, cacheDirectoryName).apply { mkdirs() }
    return File.createTempFile(
        "${fileNamePrefix}_${System.currentTimeMillis()}_",
        extension,
        imageDirectory,
    )
}

/**
 * 촬영본을 담을 파일과 그 공유 URI 를 만든다.
 *
 * [cacheDirectoryName] 은 **`res/xml/profile_image_paths.xml` 에 등록된 이름이어야 한다.**
 * 등록되지 않은 폴더면 `FileProvider` 가 "Failed to find configured root" 로 던진다.
 * 실패해도 부르는 쪽은 아무 일도 하지 않으므로 - 카메라가 뜨지 않을 뿐 오류도 안 보인다 -
 * 사유를 남긴다.
 */
private fun createImageCaptureUri(
    context: Context,
    cacheDirectoryName: String,
    fileNamePrefix: String,
): Uri? = runCatching {
    val imageFile = createCacheFile(
        context = context,
        cacheDirectoryName = cacheDirectoryName,
        fileNamePrefix = fileNamePrefix,
        extension = ".$DEFAULT_PHOTO_EXTENSION",
    )
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile,
    )
}
    .onFailure { throwable ->
        Log.e("ImagePicker", "촬영 파일 URI 를 만들지 못했습니다: $cacheDirectoryName", throwable)
    }
    .getOrNull()
