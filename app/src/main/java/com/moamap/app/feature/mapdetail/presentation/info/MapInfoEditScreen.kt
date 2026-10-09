package com.moamap.app.feature.mapdetail.presentation.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MapFormInputField
import com.moamap.app.core.designsystem.component.MapFormSubmitButton
import com.moamap.app.core.designsystem.component.MapTagChipRow
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.presentation.createmap.MapCoverPickerField
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.collectLatest

/** 버튼과 홈 인디케이터 사이 간격. 새 지도 만들기와 같다. */
private val SaveButtonBottomPadding = 13.dp

/** 고정된 버튼에 마지막 입력이 가리지 않도록 확보하는 높이. */
private val SaveButtonAreaHeight = 80.dp

/**
 * 지도 정보 수정. 지도 정보 화면 오른쪽 위 연필로 들어온다.
 *
 * 새 지도 만들기에서 공개 범위·URL 허용·외부 지도를 뺀 모양이다. 공개 범위는 서버가 바꾸지 못한다.
 * 키보드·하단 버튼 배치도 새 지도 만들기(`CreateMapContent`)와 같다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MapInfoEditScreen(
    form: MapInfoForm,
    saving: Boolean,
    canSave: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onPhotoSelected: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTagInputChange: (String) -> Unit,
    onTagCommit: () -> Unit,
    onTagRemove: (String) -> Unit,
    onSaveClick: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isKeyboardVisible = WindowInsets.isImeVisible

    val scrollState = rememberScrollState()
    var isTagFieldFocused by remember { mutableStateOf(false) }
    val tagBringIntoViewRequester = remember { BringIntoViewRequester() }

    // 맨 아래 태그 칸은 키보드가 뜨면 버튼 뒤로 숨는다. 키보드에 맞춰 줄어드는 동안 계속 끌어올린다.
    LaunchedEffect(isKeyboardVisible, isTagFieldFocused) {
        if (!isKeyboardVisible || !isTagFieldFocused) return@LaunchedEffect

        snapshotFlow { scrollState.maxValue }
            .collectLatest { tagBringIntoViewRequester.bringIntoView() }
    }

    // 사진 고르기 메뉴가 이 화면을 흐려 바탕으로 깐다.
    val photoMenuHazeState = rememberHazeState()

    // 키보드가 떠도 창이 줄지 않아(enableEdgeToEdge) 화면 전체에 imePadding 을 건다 - CreateMapContent 참고.
    // 배경 탭을 받는 수식어가 뒤에 깔린 지도로 터치가 새는 것도 막는다.
    Box(
        modifier = modifier
            .dismissKeyboardOnBackgroundTap()
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize().hazeSource(photoMenuHazeState)) {
            MoaMapTitleTopBar(title = "지도 정보 수정", onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = SaveButtonAreaHeight)
                    .verticalScroll(scrollState)
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
                    .padding(top = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = "지도 이름과 소개, 태그를 수정할 수 있어요",
                    style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                    color = MoaMapTheme.colors.textNormal,
                    modifier = Modifier.padding(start = 4.dp),
                )

                MapCoverPickerField(
                    photo = form.photo,
                    onPhotoSelected = onPhotoSelected,
                    enabled = !saving,
                    hazeState = photoMenuHazeState,
                    showsEditOverlay = true,
                )

                MapFormInputField(
                    label = "지도 이름",
                    value = form.name,
                    onValueChange = onNameChange,
                    placeholder = "지도 이름을 입력해주세요",
                )

                MapFormInputField(
                    label = "지도 설명",
                    value = form.description,
                    onValueChange = onDescriptionChange,
                    placeholder = "지도 설명을 입력해주세요",
                )

                MapFormInputField(
                    label = "태그",
                    value = form.tagInput,
                    onValueChange = onTagInputChange,
                    placeholder = "태그 입력 후 엔터",
                    modifier = Modifier
                        .bringIntoViewRequester(tagBringIntoViewRequester)
                        .onFocusChanged { isTagFieldFocused = it.hasFocus },
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { onTagCommit() }),
                    betweenLabelAndInput = if (form.tags.isEmpty()) {
                        null
                    } else {
                        { MapTagChipRow(tags = form.tags, onRemoveTag = onTagRemove) }
                    },
                )
            }
        }

        // 안내가 버튼에 가리지 않도록 버튼 위에 쌓는다.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        ) {
            ErrorSnackbar(message = errorMessage, onShown = onErrorShown)

            MapFormSubmitButton(
                label = "저장하기",
                enabled = canSave,
                submitting = saving,
                onClick = onSaveClick,
                modifier = Modifier
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
                    .padding(
                        top = SaveButtonBottomPadding,
                        // 키보드가 올라와 있으면 버튼이 키보드에 붙는다.
                        bottom = if (isKeyboardVisible) 0.dp else SaveButtonBottomPadding,
                    ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapInfoEditScreenPreview() {
    val form = MapInfoForm(
        original = MapDetail(
            id = 1L,
            title = "숭실대 주변 맛집",
            description = "학교 주변에서 함께 찾아본 맛집을 모아봤어요",
            imageUrl = null,
            ownerName = "모아",
            type = MapType.Community,
            role = MapRole.Owner,
            tags = listOf("맛집", "숭실대", "점심"),
            memberCount = 3,
            placeCount = 12,
            joined = true,
            personal = false,
            inviteCode = null,
        ),
    )
    MoaMapTheme {
        MapInfoEditScreen(
            form = form,
            saving = false,
            canSave = form.changed,
            errorMessage = null,
            onBackClick = {},
            onPhotoSelected = {},
            onNameChange = {},
            onDescriptionChange = {},
            onTagInputChange = {},
            onTagCommit = {},
            onTagRemove = {},
            onSaveClick = {},
            onErrorShown = {},
        )
    }
}
