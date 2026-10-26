package com.moamap.app.feature.mapdetail.presentation.placeedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.presentation.addplace.AddPlaceHorizontalPadding
import com.moamap.app.feature.mapdetail.presentation.addplace.PlaceFormContent
import com.moamap.app.feature.mapdetail.presentation.addplace.SubmitButton

/**
 * 장소 수정. 장소 페이지 ⋮ 「수정하기」로 들어오고 지도 상세 위를 덮는다. 시안 릴리즈 v1 「장소 수정하기 -
 * 사진 X/O」·「장소 수정 관련」 메모.
 *
 * 장소 추가 등록 폼과 같은 모양에서 사진 칸만 뺐다 - 서버가 장소 사진을 바꾸지 못한다. 바뀐 게 없으면
 * [저장하기]가 회색으로 막힌다.
 *
 * @param onBackClick `←`. 기기 뒤로가기도 같은 길이라 부르는 쪽이 한 곳에서 정한다.
 */
@Composable
internal fun PlaceEditScreen(
    form: PlaceEditForm,
    canSave: Boolean,
    errorMessage: String?,
    discardConfirmVisible: Boolean,
    onBackClick: () -> Unit,
    onTagInputChange: (String) -> Unit,
    onTagBackspace: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onErrorShown: () -> Unit,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
) {
    // 장소 페이지보다 나중에 생겨 그쪽 뒤로가기보다 먼저 받는다. 화면을 돌려 다시 그릴 때는 지도 상세가 받는다.
    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            // 뒤에 깔린 장소 페이지로 터치가 새지 않게 빈 자리의 탭을 여기서 받는다.
            .dismissKeyboardOnBackgroundTap()
            .statusBarsPadding()
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MoaMapTitleTopBar(title = "장소 수정하기", onBackClick = onBackClick)

            Box(modifier = Modifier.weight(1f)) {
                PlaceFormContent(
                    showsTitle = false,
                    showsPhotos = false,
                    placeName = form.original.name,
                    placeAddress = form.original.address,
                    photos = emptyList(),
                    tags = form.tags,
                    tagInput = form.tagInput,
                    memo = form.memo,
                    canAddPhoto = false,
                    onAddPhotoClick = {},
                    onRemovePhoto = {},
                    onTagInputChange = onTagInputChange,
                    onTagBackspace = onTagBackspace,
                    onRemoveTag = onRemoveTag,
                    onMemoChange = onMemoChange,
                )
            }

            SubmitButton(
                label = "저장하기",
                enabled = canSave,
                onClick = onSaveClick,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = AddPlaceHorizontalPadding, end = AddPlaceHorizontalPadding, bottom = 16.dp),
            )
        }

        ErrorSnackbar(
            message = errorMessage,
            onShown = onErrorShown,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (discardConfirmVisible) {
        MoaMapConfirmDialog(
            title = "수정을 그만둘까요?",
            message = "저장하지 않은 변경사항은 사라져요",
            confirmText = "나가기",
            onConfirm = onDiscard,
            onDismissRequest = onKeepEditing,
            dismissText = "계속 수정",
            dismissColor = MoaMapPrimitiveColors.Gray200,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceEditScreenPreview() {
    val form = PlaceEditForm(
        original = MapPlace(
            id = 1L,
            name = "커피나무",
            address = "서울시 동작구 369",
            latitude = 37.5,
            longitude = 127.0,
            photoUrl = null,
            description = "창가 자리가 좋아요",
            tags = listOf("카페", "데이트"),
        ),
    )
    MoaMapTheme {
        PlaceEditScreen(
            form = form,
            canSave = form.changed,
            errorMessage = null,
            discardConfirmVisible = false,
            onBackClick = {},
            onTagInputChange = {},
            onTagBackspace = {},
            onRemoveTag = {},
            onMemoChange = {},
            onSaveClick = {},
            onErrorShown = {},
            onKeepEditing = {},
            onDiscard = {},
        )
    }
}
