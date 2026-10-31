package com.moamap.app.core.common.imagepicker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
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
import com.moamap.app.core.common.openAppSettings
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
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
 * 카메라 런처·권한 요청과 시스템 사진 선택기를 묶어 [ImagePickerController] 로 돌려준다.
 *
 * 촬영본은 [cacheDirectoryName] 아래에 만들어 [FileProvider] 로 넘긴다. 새 디렉터리를 쓰려면
 * `res/xml/profile_image_paths.xml` 에 `cache-path` 를 함께 추가해야 한다.
 *
 * 갤러리는 시스템 사진 선택기(Photo Picker)를 띄운다. Google Play 정책상 사진 첨부만 하는 앱은
 * `READ_MEDIA_IMAGES` 로 기기 사진을 직접 읽을 수 없어서다(#412). 사진 접근 권한이 필요 없고,
 * 선택기가 없는 기기(11 미만, Play 서비스 백포트도 없음)에서는 androidx 가 문서 선택기로 대신 띄운다.
 * 고른 사진은 앱이 다시 살아나도 읽히게 지속 읽기 권한을 받아 둔다. 받지 못하는 주소도 있어 실패는 넘긴다.
 * 형식은 선택기가 거르지 못하므로 올릴 때 검사한다(`requireUploadableSourceType`).
 *
 * 여러 장을 고르면 [onImageSelected] 를 고른 순서대로 한 장씩 부른다.
 *
 * 컨트롤러는 recomposition 마다 새로 만든다 - 붙잡고 있는 런처들은 이미 remember 된 값이라
 * 새로 만들어도 같은 대상을 가리키고, 클릭 시점에 최신 [onImageSelected] 를 보게 하려면
 * 오히려 붙잡아두지 않는 편이 안전하다.
 *
 * @param maxSelectable 갤러리에서 한 번에 고를 수 있는 장수. 0 이하면 갤러리를 열지 않는다.
 */
@Composable
internal fun rememberImagePickerController(
    state: ImagePickerState,
    cacheDirectoryName: String,
    fileNamePrefix: String,
    maxSelectable: Int = 1,
    onImageSelected: (Uri) -> Unit = {},
): ImagePickerController {
    val context = LocalContext.current
    val currentOnImageSelected by rememberUpdatedState(onImageSelected)
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraDeniedVisible by rememberSaveable { mutableStateOf(false) }

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

    val onPicked: (List<Uri>) -> Unit = { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            currentOnImageSelected(uri)
        }
    }

    // 고르지 않고 닫으면 null·빈 목록이 온다.
    val singlePhotoLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
    ) { uri -> onPicked(listOfNotNull(uri)) }

    // 여러 장 선택기는 최대 장수가 2 이상이어야 한다. 장수는 남은 자리에 따라 바뀌어 매번 새로 넘긴다.
    val multiplePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxSelectable.coerceAtLeast(2)),
        onResult = onPicked,
    )

    val requestGallery: () -> Unit = {
        state.dismissSourceMenu()
        val request = PickVisualMediaRequest(PickVisualMedia.ImageOnly)
        when {
            maxSelectable <= 0 -> Unit
            maxSelectable == 1 -> singlePhotoLauncher.launch(request)
            else -> multiplePhotoLauncher.launch(request)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // 거부하면 안내한다. 두 번 거부한 뒤에는 시스템 팝업 없이 바로 여기로 온다.
        if (granted) launchCamera() else cameraDeniedVisible = true
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

    if (cameraDeniedVisible) {
        MoaMapConfirmDialog(
            title = "카메라 권한이 필요해요",
            // 한 줄로 두면 「찍을 / 수」처럼 단어 중간에서 꺾인다.
            message = "설정에서 카메라 접근을 허용하면\n사진을 찍을 수 있어요.",
            confirmText = "설정 열기",
            onConfirm = {
                cameraDeniedVisible = false
                context.openAppSettings()
            },
            onDismissRequest = { cameraDeniedVisible = false },
            dismissText = "닫기",
            dismissColor = MoaMapPrimitiveColors.Gray200,
        )
    }

    return ImagePickerController(
        onRequestCamera = requestCamera,
        onRequestGallery = requestGallery,
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
