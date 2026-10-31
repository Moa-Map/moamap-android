package com.moamap.app.core.common.imagepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * 카메라/갤러리 선택 메뉴의 노출 상태. 갤러리는 시스템 사진 선택기라 여기서 들고 있지 않는다.
 *
 * 고른 사진은 여기에 두지 않는다. 화면마다 사진을 들고 있는 곳(ViewModel 이든 화면 상태든)이
 * 따로 있어서, 여기에도 두면 같은 값이 두 군데에 생기고 저장 수명이 어긋난다.
 * 고른 결과는 [rememberImagePickerController] 의 `onImageSelected` 로만 나간다.
 */
@Stable
internal class ImagePickerState(
    initiallyVisible: Boolean = false,
) {
    var isSourceMenuVisible by mutableStateOf(initiallyVisible)
        private set

    fun showSourceMenu() {
        isSourceMenuVisible = true
    }

    fun dismissSourceMenu() {
        isSourceMenuVisible = false
    }
}

private val ImagePickerStateSaver = listSaver<ImagePickerState, Boolean>(
    save = { listOf(it.isSourceMenuVisible) },
    restore = { ImagePickerState(initiallyVisible = it[0]) },
)

@Composable
internal fun rememberImagePickerState(): ImagePickerState = rememberSaveable(
    saver = ImagePickerStateSaver,
) {
    ImagePickerState()
}
