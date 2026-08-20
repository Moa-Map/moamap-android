package com.moamap.app.feature.mapdetail.presentation.addplace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.common.imagepicker.rememberImagePickerController
import com.moamap.app.core.common.imagepicker.rememberImagePickerState
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ButtonShadowColor
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.ImageSourceMenu
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.MapDetail

internal val AddPlaceHorizontalPadding = 20.dp

/**
 * 시트 위에 남기는 여백.
 *
 * 시트는 아래에 붙어 위로 자라므로, 내용 높이를 화면에서 이만큼 뺀 값으로 고정해
 * 윗변이 늘 같은 자리에 서게 한다. 내용에 맡기면 검색 결과 수에 따라 높이가 출렁인다.
 *
 * 두 단계의 높이는 다르다. 검색은 결과 네 장까지만 담고 뒤의 지도를 조금 남겨 두는데,
 * 등록 폼은 사진·태그·메모에 등록 버튼까지 얹어야 해서 화면을 거의 다 쓴다.
 */
private val SearchStepTopMargin = 104.dp
private val FormStepTopMargin = 28.dp

private val SheetShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
private val GrabberShape = RoundedCornerShape(100.dp)
private val SubmitButtonShape = RoundedCornerShape(8.dp)

/** 촬영본을 담는 캐시 폴더. `res/xml/profile_image_paths.xml` 에 같은 이름이 있어야 한다. */
/**
 * 촬영본을 담아 둘 캐시 폴더.
 *
 * **`res/xml/profile_image_paths.xml` 에 등록된 이름이어야 한다.** 등록되지 않은 폴더를 쓰면
 * `FileProvider` 가 URI 를 만들지 못하고, 그 실패가 삼켜져 카메라가 조용히 안 뜬다.
 *
 * 링크로 가져온 장소를 편집할 때도 같은 폴더를 쓴다. 담기는 것이 똑같이 장소 사진이고,
 * 이 폴더들을 비우는 코드가 어디에도 없어 서로 간섭하지 않는다. 파일 이름 앞머리로 구분한다.
 */
internal const val PLACE_PHOTO_CACHE_DIRECTORY = "place_photos"

/**
 * 장소 추가 시트.
 *
 * 검색과 등록 폼 두 단계가 한 시트 안에서 오간다. 단계는 `selected` 하나로 갈린다.
 *
 * @param onAdded 등록이 끝났다. 안내 문구를 받아 상세 화면이 띄운다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddPlaceSheet(
    map: MapDetail,
    onDismiss: () -> Unit,
    onAdded: (String) -> Unit,
    viewModel: AddPlaceViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.addedMessage) {
        uiState.addedMessage?.let(onAdded)
    }

    val pickerState = rememberImagePickerState()
    val pickerController = rememberImagePickerController(
        state = pickerState,
        cacheDirectoryName = PLACE_PHOTO_CACHE_DIRECTORY,
        fileNamePrefix = "place",
        onImageSelected = viewModel::addPhoto,
    )

    // 화면 높이에서 단계별 여백을 뺀 만큼으로 고정한다. 장소를 고르면 시트가 폼 높이로 자란다.
    val topMargin = if (uiState.isFormStep) FormStepTopMargin else SearchStepTopMargin
    val sheetHeight = LocalConfiguration.current.screenHeightDp.dp - topMargin

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = MoaMapTheme.colors.backgroundSecondary,
        tonalElevation = 0.dp,
        dragHandle = { SheetGrabber() },
        // 시트가 상태바 아래까지 올라올 수 있어야 윗변 위치를 우리가 정할 수 있다.
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isFormStep) {
                    FormToolbar(
                        onBackClick = viewModel::backToSearch,
                        onCloseClick = onDismiss,
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    val selected = uiState.selected
                    if (selected == null) {
                        PlaceSearchContent(
                            query = uiState.query,
                            search = uiState.search,
                            onQueryChange = viewModel::updateQuery,
                            onRetryClick = viewModel::retrySearch,
                            onCandidateClick = viewModel::selectCandidate,
                        )
                    } else {
                        PlaceFormContent(
                            placeName = selected.name,
                            placeAddress = selected.displayAddress,
                            photos = uiState.photos,
                            tags = uiState.tags,
                            tagInput = uiState.tagInput,
                            memo = uiState.memo,
                            canAddPhoto = uiState.canAddPhoto,
                            onAddPhotoClick = pickerState::showSourceMenu,
                            onRemovePhoto = viewModel::removePhoto,
                            onTagInputChange = viewModel::updateTagInput,
                            onTagBackspace = viewModel::removeLastTagIfInputEmpty,
                            onRemoveTag = viewModel::removeTag,
                            onMemoChange = viewModel::updateMemo,
                        )
                    }
                }

                if (uiState.isFormStep) {
                    SubmitButton(
                        label = addPlaceButtonLabel(map),
                        enabled = !uiState.submitting,
                        onClick = { viewModel.submit(map) },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(
                                start = AddPlaceHorizontalPadding,
                                end = AddPlaceHorizontalPadding,
                                bottom = 16.dp,
                            ),
                    )
                }
            }

            // 카메라·갤러리 고르기. 시트 안에 겹쳐 띄운다 - Popup 으로 띄우면 시트 밖에
            // 그려져 바깥을 눌렀을 때 시트까지 함께 닫힌다.
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

            ErrorSnackbar(
                message = uiState.errorMessage,
                onShown = viewModel::consumeErrorMessage,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun SheetGrabber() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(25.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 5.dp)
                .background(MoaMapPrimitiveColors.Gray100, GrabberShape),
        )
    }
}

/** `‹` 는 검색으로 되돌아가고 `×` 는 흐름을 닫는다. */
@Composable
private fun FormToolbar(
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AddPlaceHorizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_left),
            contentDescription = "검색으로",
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBackClick),
        )
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = "닫기",
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onCloseClick),
        )
    }
}

@Composable
private fun SubmitButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = SubmitButtonShape,
        // 비활성은 전송 중일 때뿐이다. 입력값은 모두 선택이라 처음부터 누를 수 있다.
        color = if (enabled) MoaMapPrimitiveColors.Blue500 else MoaMapPrimitiveColors.Gray200,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ButtonShadowColor,
        onClick = { if (enabled) onClick() },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MoaMapTheme.typography.button0,
                color = MoaMapTheme.colors.textWhite,
            )
        }
    }
}
