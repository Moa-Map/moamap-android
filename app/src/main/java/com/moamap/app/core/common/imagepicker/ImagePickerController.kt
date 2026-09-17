package com.moamap.app.core.common.imagepicker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 형식을 알아내지 못했을 때 붙일 확장자. 촬영본도 이 확장자를 쓴다. */
private const val DEFAULT_PHOTO_EXTENSION = "jpg"

/** 형식을 좁히지 않은 기본값. 선택기에는 "사진 전체" 로 넘긴다. */
private const val ANY_IMAGE_MIME_TYPE = "image/*"

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
 * 갤러리는 시스템 사진 선택기를 띄운다. 파일 선택기와 달리 사진이 곧바로 격자로 보이고,
 * 저장소·사진 권한도 필요 없다. 고른 사진은 캐시로 복사해 두고 그 사본을 넘긴다 -
 * 사진 선택기가 주는 권한은 지속되지 않아, 앱이 죽었다 살아나면 원본을 다시 읽을 수 없다.
 *
 * [mimeTypes] 는 형식이 하나일 때만 선택기에 전해진다. 여럿이면 사진 전체를 보여주고,
 * 서버가 받지 않는 형식은 올리기 전 검증이 거른다. 촬영본은 항상 JPEG 라 영향이 없다.
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
    mimeTypes: Array<String> = arrayOf(ANY_IMAGE_MIME_TYPE),
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

    val scope = rememberCoroutineScope()
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        // 사진 선택기는 지속 권한을 주지 않는다. 골라 둔 채 앱이 죽었다 살아나면 다시 읽을 수
        // 없으므로, 고른 즉시 캐시로 복사해 우리 파일을 넘긴다.
        scope.launch {
            val copied = copyToCache(context, uri, cacheDirectoryName, fileNamePrefix)
            selectImage(copied ?: uri)
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
        // 사진 선택기도 고른 사진에만 읽기 권한을 주므로 저장소·사진 권한이 필요 없다.
        // 권한을 물어보면 거부당했을 때 권한 없이도 동작할 선택기까지 막힌다.
        onRequestGallery = {
            state.dismissSourceMenu()
            galleryLauncher.launch(PickVisualMediaRequest(visualMediaTypeOf(mimeTypes)))
        },
    )
}

/**
 * 사진 선택기에 넘길 형식.
 *
 * 선택기는 형식을 하나만 받는다. 여러 형식을 받는 화면은 사진 전체를 보여주고, 서버가 받지
 * 않는 형식은 올리기 전 검증(`validateImageUpload`)이 거른다.
 */
internal fun visualMediaTypeOf(
    mimeTypes: Array<String>,
): ActivityResultContracts.PickVisualMedia.VisualMediaType {
    val single = mimeTypes.singleOrNull()
    return if (single != null && single != ANY_IMAGE_MIME_TYPE) {
        ActivityResultContracts.PickVisualMedia.SingleMimeType(single)
    } else {
        ActivityResultContracts.PickVisualMedia.ImageOnly
    }
}

/**
 * 고른 사진을 캐시로 복사하고 우리 [FileProvider] 주소를 돌려준다. 실패하면 null 이다.
 *
 * 확장자는 원본 형식에서 정한다. `ContentResolver` 가 우리 파일의 형식을 확장자로 판단하므로,
 * PNG 를 `.jpg` 로 복사해 두면 업로드할 때 형식이 어긋난다. 서버가 받지 않는 형식(HEIC 등)도
 * 제 확장자로 남겨야 올리기 전 검증이 제대로 걸러 낸다.
 */
private suspend fun copyToCache(
    context: Context,
    source: Uri,
    cacheDirectoryName: String,
    fileNamePrefix: String,
): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(resolver.getType(source))
            ?: DEFAULT_PHOTO_EXTENSION
        val target = createCacheFile(context, cacheDirectoryName, fileNamePrefix, ".$extension")

        resolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("고른 사진을 열 수 없습니다")

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
    }
        .onFailure { throwable ->
            Log.e("ImagePicker", "고른 사진을 캐시로 복사하지 못했습니다", throwable)
        }
        .getOrNull()
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
