package com.moamap.app.feature.collection.presentation.createmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.common.imagepicker.rememberImagePickerController
import com.moamap.app.core.common.imagepicker.rememberImagePickerState
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.ImageSourceMenu
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.common.upload.ALLOWED_IMAGE_CONTENT_TYPES
import com.moamap.app.feature.collection.domain.model.MapVisibility
import kotlinx.coroutines.flow.collectLatest

/** 촬영본이 쌓이는 캐시 위치. `res/xml/profile_image_paths.xml` 의 `cache-path` 와 맞춰야 한다. */
private const val MapImageCacheDirectory = "map_images"
private const val MapImageFilePrefix = "map"

/**
 * 갤러리에 보일 형식.
 *
 * 발급 전에 거르는 형식과 같은 값을 써야 한다. 고르고 나서 거절당하지 않도록 선택기에서
 * 미리 좁히는 것뿐이라, 목록을 따로 두면 서버 계약이 바뀔 때 조용히 어긋난다.
 */
private val CoverImageMimeTypes = ALLOWED_IMAGE_CONTENT_TYPES.toTypedArray()

/** 버튼과 홈 인디케이터 사이 간격. */
private val SubmitButtonBottomPadding = 13.dp

/** 고정된 버튼에 마지막 입력이 가리지 않도록 확보하는 높이. */
private val SubmitButtonAreaHeight = 80.dp

@Composable
internal fun CreateMapScreen(
    onBackClick: () -> Unit,
    onCreated: (mapId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val submit = uiState.submit
    LaunchedEffect(submit) {
        if (submit is SubmitState.Done) onCreated(submit.mapId)
    }

    if (submit is SubmitState.ShowingInviteCode) {
        InviteCodeDialog(
            mapName = uiState.name,
            inviteCode = submit.inviteCode,
            onDismiss = viewModel::dismissInviteCode,
        )
    }

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
        onSubmitClick = viewModel::submit,
        onErrorShown = viewModel::consumeError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
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
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isKeyboardVisible = WindowInsets.isImeVisible

    val scrollState = rememberScrollState()
    var isTagFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isKeyboardVisible, isTagFieldFocused) {
        if (!isKeyboardVisible || !isTagFieldFocused) return@LaunchedEffect

        snapshotFlow { scrollState.maxValue }
            .collectLatest { maxValue -> scrollState.animateScrollTo(maxValue) }
    }

    val pickerState = rememberImagePickerState()
    val pickerController = rememberImagePickerController(
        state = pickerState,
        cacheDirectoryName = MapImageCacheDirectory,
        fileNamePrefix = MapImageFilePrefix,
        mimeTypes = CoverImageMimeTypes,
        onImageSelected = { uri -> onImageSelected(uri.toString()) },
    )

    // enableEdgeToEdge 라 키보드가 떠도 창이 줄지 않는다. imePadding 을 화면 전체에 걸어야
    // 스크롤 영역의 뷰포트가 함께 줄어들어, 포커스된 입력창이 키보드 위로 올라온다.
    // 버튼에만 걸면 버튼 혼자 키보드를 타고 올라가고 입력창은 가려진 채로 남는다.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreateMapTopBar(onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 버튼과 같은 여백을 스크롤 영역에서 빼둔다. 키보드가 떠 있으면
                    // 위에서 ime inset 을 소비해 내비게이션 바 몫은 0 이 된다.
                    .navigationBarsPadding()
                    .padding(bottom = SubmitButtonAreaHeight)
                    .verticalScroll(scrollState)
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(20.dp))

                Box {
                    MapPhotoField(
                        imageUri = uiState.imageUri,
                        // 올리는 중에는 잠근다. 선택기를 띄워놓고 고른 값을 버리면
                        // 왜 안 바뀌는지 알 수 없다.
                        onClick = {
                            if (!uiState.isSubmitting) pickerState.showSourceMenu()
                        },
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
                    modifier = Modifier.onFocusChanged { isTagFieldFocused = it.hasFocus },
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

                Spacer(Modifier.height(20.dp))
            }
        }

        // 안내가 버튼에 가리지 않도록 버튼 위에 쌓는다.
        Column(
            modifier = Modifier
                // 키보드가 떠 있으면 위에서 ime inset 을 이미 소비해 0 이 되고,
                // 닫혀 있을 때만 내비게이션 바만큼 띄운다.
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        ) {
            ErrorSnackbar(message = uiState.errorMessage, onShown = onErrorShown)

            CreateMapSubmitButton(
                enabled = uiState.canSubmit,
                submitting = uiState.isSubmitting,
                onClick = onSubmitClick,
                modifier = Modifier
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
                    .padding(
                        top = SubmitButtonBottomPadding,
                        // 홈 인디케이터와 띄우려는 간격이라, 그 자리에 키보드가 올라와 있으면
                        // 버튼이 키보드에 붙어야 한다.
                        bottom = if (isKeyboardVisible) 0.dp else SubmitButtonBottomPadding,
                    ),
            )
        }
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
            onErrorShown = {},
        )
    }
}
