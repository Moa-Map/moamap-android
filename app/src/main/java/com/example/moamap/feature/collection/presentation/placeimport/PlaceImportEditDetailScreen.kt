package com.example.moamap.feature.collection.presentation.placeimport

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import com.example.moamap.core.common.imagepicker.rememberImagePickerController
import com.example.moamap.core.common.imagepicker.rememberImagePickerState
import com.example.moamap.core.designsystem.component.ImageSourceMenu
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.PlaceEdit
import com.example.moamap.feature.mapdetail.presentation.addplace.MAX_PLACE_PHOTOS
import com.example.moamap.feature.mapdetail.presentation.addplace.PLACE_PHOTO_CACHE_DIRECTORY
import com.example.moamap.feature.mapdetail.presentation.addplace.PlaceFormContent
import com.example.moamap.feature.mapdetail.presentation.addplace.applyTagInput
import com.example.moamap.feature.mapdetail.presentation.addplace.removeLastTag

/**
 * 고른 장소 하나에 사진·태그·메모를 붙이는 화면.
 *
 * 폼은 지도 상세의 장소 추가와 같은 [PlaceFormContent] 다. 시안(Figma 1841-11938)은 지도 위에
 * 뜨는 바텀시트지만, 이 흐름에는 뒤에 깔릴 지도가 없고 저장할 지도도 아직 고르기 전이라
 * 다른 단계와 같은 전체 화면으로 만든다.
 *
 * 편집은 선택이므로 되돌리기 버튼이 따로 없다. 값은 바꾸는 즉시 반영되고 뒤로가기로 나간다.
 */
@Composable
internal fun PlaceImportEditDetailScreen(
    place: ImportedPlace,
    edit: PlaceEdit,
    onBackClick: () -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onMemoChange: (String) -> Unit,
    onAddPhoto: (Uri) -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 확정되지 않은 입력 중인 글자다. 태그가 되기 전까지는 저장할 값이 아니라 화면이 들고 있는다.
    var tagInput by rememberSaveable(place.id) { mutableStateOf("") }

    val pickerState = rememberImagePickerState()
    val pickerController = rememberImagePickerController(
        state = pickerState,
        // 새 이름을 쓰면 FileProvider 가 URI 를 못 만들어 카메라가 조용히 안 뜬다.
        cacheDirectoryName = PLACE_PHOTO_CACHE_DIRECTORY,
        fileNamePrefix = "import_place",
        onImageSelected = onAddPhoto,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceImportTopBar(onBackClick = onBackClick)

            Box(modifier = Modifier.weight(1f)) {
                PlaceFormContent(
                    placeName = place.name,
                    placeAddress = place.displayAddress,
                    photos = edit.photos,
                    tags = edit.tags,
                    tagInput = tagInput,
                    memo = edit.memo,
                    canAddPhoto = edit.photos.size < MAX_PLACE_PHOTOS,
                    onAddPhotoClick = pickerState::showSourceMenu,
                    onRemovePhoto = onRemovePhoto,
                    onTagInputChange = { input ->
                        val result = applyTagInput(tags = edit.tags, rawInput = input)
                        tagInput = result.input
                        // 구분자가 없으면 확정된 태그가 없다. 그때마다 상태를 건드리지 않는다.
                        if (result.tags != edit.tags) onTagsChange(result.tags)
                    },
                    onTagBackspace = {
                        if (tagInput.isEmpty()) onTagsChange(removeLastTag(edit.tags))
                    },
                    onRemoveTag = { tag -> onTagsChange(edit.tags - tag) },
                    onMemoChange = onMemoChange,
                )
            }

            PlaceImportBottomBar(modifier = Modifier.imePadding()) {
                PlaceImportPrimaryButton(
                    text = "완료",
                    enabled = true,
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 카메라·갤러리 고르기. 바깥을 누르면 닫힌다.
        if (pickerState.isSourceMenuVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { pickerState.dismissSourceMenu() }
                    },
                contentAlignment = Alignment.Center,
            ) {
                ImageSourceMenu(
                    onCameraClick = pickerController::requestCamera,
                    onGalleryClick = pickerController::requestGallery,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportEditDetailScreenPreview() {
    MoaMapTheme {
        PlaceImportEditDetailScreen(
            place = ImportedPlace(
                id = "1",
                name = "커피나무",
                roadAddress = "서울시 동작구 369",
            ),
            edit = PlaceEdit(tags = listOf("성수", "카페"), memo = "재방문 의사 있음"),
            onBackClick = {},
            onTagsChange = {},
            onMemoChange = {},
            onAddPhoto = {},
            onRemovePhoto = {},
        )
    }
}
