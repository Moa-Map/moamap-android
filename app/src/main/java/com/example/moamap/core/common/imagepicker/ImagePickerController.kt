package com.example.moamap.core.common.imagepicker

import android.Manifest
import android.content.Context
import android.content.Intent
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
import java.io.File

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
 * 카메라·갤러리 런처와 권한 요청을 묶어 [ImagePickerController] 로 돌려준다.
 *
 * 촬영본은 [cacheDirectoryName] 아래에 만들어 [FileProvider] 로 넘긴다. 새 디렉터리를 쓰려면
 * `res/xml/profile_image_paths.xml` 에 `cache-path` 를 함께 추가해야 한다.
 *
 * [mimeTypes] 로 갤러리에 보일 형식을 좁힐 수 있다. 서버가 받는 형식이 정해져 있으면 고르고
 * 나서 거절당하는 것보다 애초에 안 보이는 편이 낫다. 촬영본은 항상 JPEG 라 영향이 없다.
 *
 * 컨트롤러는 recomposition 마다 새로 만든다 - 붙잡고 있는 런처들은 이미 remember 된 값이라
 * 새로 만들어도 같은 대상을 가리키고, 클릭 시점에 최신 [onImageSelected] 를 보게 하려면
 * 오히려 붙잡아두지 않는 편이 안전하다.
 */
@Composable
internal fun rememberImagePickerController(
    state: ImagePickerState,
    cacheDirectoryName: String,
    fileNamePrefix: String,
    mimeTypes: Array<String> = arrayOf("image/*"),
    onImageSelected: (Uri) -> Unit = {},
): ImagePickerController {
    val context = LocalContext.current
    val currentOnImageSelected by rememberUpdatedState(onImageSelected)
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    fun selectImage(uri: Uri) {
        state.dismissSourceMenu()
        currentOnImageSelected(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        pendingCameraUri?.let(Uri::parse)?.let { uri ->
            if (success) {
                selectImage(uri)
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = PersistableOpenDocument,
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            selectImage(it)
        }
    }

    return ImagePickerController(
        onRequestCamera = {
            state.dismissSourceMenu()
            if (
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        // 문서 선택기(SAF)는 고른 URI 에만 그때그때 읽기 권한을 부여하므로 저장소 권한이
        // 필요 없다. 권한을 물어보면 거부당했을 때 권한 없이도 동작할 선택기까지 막힌다.
        onRequestGallery = {
            state.dismissSourceMenu()
            galleryLauncher.launch(mimeTypes)
        },
    )
}

/**
 * [ActivityResultContracts.OpenDocument] 에 지속 권한 플래그를 더한 계약.
 *
 * 기본 계약은 이 플래그를 붙이지 않아서 `takePersistableUriPermission` 이 SecurityException 을
 * 낸다. 프로세스가 죽었다 살아난 뒤 복원한 URI 를 다시 읽으려면 지속 권한이 필요하다.
 */
private object PersistableOpenDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input)
            .addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
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
    val imageDirectory = File(context.cacheDir, cacheDirectoryName).apply { mkdirs() }
    val imageFile = File.createTempFile(
        "${fileNamePrefix}_${System.currentTimeMillis()}_",
        ".jpg",
        imageDirectory,
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
