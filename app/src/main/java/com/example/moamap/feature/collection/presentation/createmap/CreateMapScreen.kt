package com.example.moamap.feature.collection.presentation.createmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moamap.R
import com.example.moamap.core.common.imagepicker.rememberImagePickerController
import com.example.moamap.core.common.imagepicker.rememberImagePickerState
import com.example.moamap.core.designsystem.component.ImageSourceMenu
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapTheme

/** 촬영본이 쌓이는 캐시 위치. `res/xml/profile_image_paths.xml` 의 `cache-path` 와 맞춰야 한다. */
private const val MapImageCacheDirectory = "map_images"
private const val MapImageFilePrefix = "map"

/** 버튼과 홈 인디케이터 사이 간격. */
private val SubmitButtonBottomPadding = 13.dp

/** 고정된 버튼에 마지막 입력이 가리지 않도록 확보하는 높이. */
private val ContentBottomSpacing = 90.dp

@Composable
internal fun CreateMapScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CreateMapContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onImageSelected = viewModel::selectImage,
        onNameChange = viewModel::updateName,
        onDescriptionChange = viewModel::updateDescription,
        onVisibilitySelect = viewModel::selectVisibility,
        onTagInputChange = viewModel::updateTagInput,
        onTagCommit = viewModel::commitTag,
        onTagRemove = viewModel::removeTag,
        // TODO: POST /api/v1/maps 연결은 다음 이슈에서 붙인다.
        onSubmitClick = {},
        modifier = modifier,
    )
}

@Composable
private fun CreateMapContent(
    uiState: CreateMapUiState,
    onBackClick: () -> Unit,
    onImageSelected: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onVisibilitySelect: (MapVisibility) -> Unit,
    onTagInputChange: (String) -> Unit,
    onTagCommit: () -> Unit,
    onTagRemove: (String) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickerState = rememberImagePickerState()
    val pickerController = rememberImagePickerController(
        state = pickerState,
        cacheDirectoryName = MapImageCacheDirectory,
        fileNamePrefix = MapImageFilePrefix,
        onImageSelected = { uri -> onImageSelected(uri.toString()) },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreateMapTopBar(onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(20.dp))

                Box {
                    MapPhotoField(
                        imageUri = uiState.imageUri,
                        onClick = pickerState::showSourceMenu,
                    )

                    if (pickerState.isSourceMenuVisible) {
                        Popup(
                            alignment = Alignment.BottomCenter,
                            onDismissRequest = pickerState::dismissSourceMenu,
                            properties = PopupProperties(focusable = true),
                        ) {
                            ImageSourceMenu(
                                onCameraClick = pickerController::requestCamera,
                                onGalleryClick = pickerController::requestGallery,
                            )
                        }
                    }
                }

                CreateMapInputField(
                    label = "지도 이름",
                    value = uiState.name,
                    onValueChange = onNameChange,
                    placeholder = "지도 이름을 입력해주세요",
                )

                CreateMapInputField(
                    label = "지도 설명",
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    placeholder = "지도 설명을 입력해주세요",
                )

                VisibilitySection(
                    selected = uiState.visibility,
                    onSelect = onVisibilitySelect,
                )

                CreateMapInputField(
                    label = "태그",
                    value = uiState.tagInput,
                    onValueChange = onTagInputChange,
                    placeholder = "태그 입력 후 스페이스 또는 엔터",
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { onTagCommit() }),
                    betweenLabelAndInput = if (uiState.tags.isEmpty()) {
                        null
                    } else {
                        {
                            TagChipRow(
                                tags = uiState.tags,
                                onRemoveTag = onTagRemove,
                            )
                        }
                    },
                )

                Spacer(Modifier.height(ContentBottomSpacing))
            }
        }

        CreateMapSubmitButton(
            enabled = uiState.canSubmit,
            onClick = onSubmitClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(
                    horizontal = MoaMapDimens.ScreenHorizontalPadding,
                    vertical = SubmitButtonBottomPadding,
                ),
        )
    }
}

@Composable
private fun CreateMapTopBar(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(48.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "뒤로가기",
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = "새 지도 만들기",
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun VisibilitySection(
    selected: MapVisibility?,
    onSelect: (MapVisibility) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "공개 범위",
            style = MoaMapTheme.typography.subtitle1,
            color = MoaMapTheme.colors.textNormal,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            VisibilityCard(
                iconRes = R.drawable.ic_language,
                title = "공개 지도",
                subtitle = "모두가 볼 수 있어요",
                selected = selected == MapVisibility.Public,
                onClick = { onSelect(MapVisibility.Public) },
                modifier = Modifier.weight(1f),
            )
            VisibilityCard(
                iconRes = R.drawable.ic_lock,
                title = "프라이빗 지도",
                subtitle = "초대한 사람만 볼 수 있어요",
                selected = selected == MapVisibility.Private,
                onClick = { onSelect(MapVisibility.Private) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun CreateMapScreenPreview() {
    MoaMapTheme {
        CreateMapContent(
            uiState = CreateMapUiState(
                name = "성수 카페 투어",
                visibility = MapVisibility.Public,
                tags = listOf("카페", "성수", "데이트"),
            ),
            onBackClick = {},
            onImageSelected = {},
            onNameChange = {},
            onDescriptionChange = {},
            onVisibilitySelect = {},
            onTagInputChange = {},
            onTagCommit = {},
            onTagRemove = {},
            onSubmitClick = {},
        )
    }
}
