package com.example.moamap.feature.mypage

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
internal class ProfileImagePickerState(
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

private val ProfileImagePickerStateSaver = listSaver<ProfileImagePickerState, Any>(
    save = {
        listOf(
            it.selectedImageUri.orEmpty(),
            it.isSourceMenuVisible,
        )
    },
    restore = {
        ProfileImagePickerState(
            initialImageUri = (it[0] as String).ifEmpty { null },
            initiallyVisible = it[1] as Boolean,
        )
    },
)

@Composable
internal fun rememberProfileImagePickerState(
    initialImageUri: Uri?,
): ProfileImagePickerState = rememberSaveable(
    initialImageUri,
    saver = ProfileImagePickerStateSaver,
) {
    ProfileImagePickerState(initialImageUri = initialImageUri?.toString())
}

internal fun galleryPermissionsFor(sdkInt: Int): List<String> = when {
    sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> listOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )

    sdkInt >= Build.VERSION_CODES.TIRAMISU -> listOf(
        Manifest.permission.READ_MEDIA_IMAGES,
    )

    else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}
