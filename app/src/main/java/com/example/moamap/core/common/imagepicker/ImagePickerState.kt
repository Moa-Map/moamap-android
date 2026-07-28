package com.example.moamap.core.common.imagepicker

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * 사진 한 장을 고르는 화면이 공유하는 상태.
 *
 * 고른 사진과 카메라/갤러리 선택 메뉴의 노출 여부만 들고 있다. 실제 런처와 권한 처리는
 * [rememberImagePickerController] 가 맡는다.
 */
@Stable
internal class ImagePickerState(
    initialImageUri: String? = null,
    initiallyVisible: Boolean = false,
) {
    var selectedImageUri by mutableStateOf(initialImageUri)
        private set

    var isSourceMenuVisible by mutableStateOf(initiallyVisible)
        private set

    fun showSourceMenu() {
        isSourceMenuVisible = true
    }

    fun dismissSourceMenu() {
        isSourceMenuVisible = false
    }

    fun selectImage(uri: String) {
        selectedImageUri = uri
        isSourceMenuVisible = false
    }
}

private val ImagePickerStateSaver = listSaver<ImagePickerState, Any>(
    save = {
        listOf(
            it.selectedImageUri.orEmpty(),
            it.isSourceMenuVisible,
        )
    },
    restore = {
        ImagePickerState(
            initialImageUri = (it[0] as String).ifEmpty { null },
            initiallyVisible = it[1] as Boolean,
        )
    },
)

@Composable
internal fun rememberImagePickerState(
    initialImageUri: Uri? = null,
): ImagePickerState = rememberSaveable(
    initialImageUri,
    saver = ImagePickerStateSaver,
) {
    ImagePickerState(initialImageUri = initialImageUri?.toString())
}

