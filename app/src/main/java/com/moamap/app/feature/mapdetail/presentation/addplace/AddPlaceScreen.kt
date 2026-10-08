package com.moamap.app.feature.mapdetail.presentation.addplace

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.common.imagepicker.rememberImagePickerController
import com.moamap.app.core.common.imagepicker.rememberImagePickerState
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ButtonShadowColor
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.ImageSourceMenu
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

internal val AddPlaceHorizontalPadding = 20.dp

private val SubmitButtonShape = RoundedCornerShape(8.dp)

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
 * 장소 추가 화면. 지도 상세 위를 덮는 한 장이다.
 *
 * 검색 → 지도에서 위치 확인 → 등록 폼 세 단계가 이 화면 안에서 오간다([AddPlaceUiState.step]).
 * 시안 「상세지도/장소추가」(검색 `4070:37922`·결과 `4070:38652`·위치 확인 `4070:38291`·등록
 * `4070:38883`). 위는 단계 제목과 `←` 뿐이다(시안, 10-09 `×` 삭제).
 *
 * @param onBackClick `←`. 한 단계씩 뒤로 - 등록 폼 → 위치 확인 → 검색 → 닫기. 기기 뒤로가기와 같은
 * 길이라 부르는 쪽이 한 곳에서 정한다.
 * @param onAdded 등록이 끝났다. 안내 문구를 받아 상세 화면이 띄운다.
 */
@Composable
internal fun AddPlaceScreen(
    map: MapDetail,
    onBackClick: () -> Unit,
    onAdded: (String) -> Unit,
    viewModel: AddPlaceViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
    // 사진 고르기 메뉴가 떠 있으면 기기 뒤로가기는 메뉴부터 닫는다. 그냥 두면 검색으로
    // 돌아간 뒤에도 메뉴가 남는다.
    BackHandler(enabled = pickerState.isSourceMenuVisible) { pickerState.dismissSourceMenu() }

    val photoMenuHazeState = rememberHazeState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            // 뒤에 깔린 지도로 터치가 새지 않게 빈 자리의 탭을 여기서 받는다.
            .dismissKeyboardOnBackgroundTap()
            .statusBarsPadding()
            .imePadding(),
    ) {
        // 사진 고르기 메뉴가 이 화면을 흐려 바탕으로 깐다.
        Column(modifier = Modifier.fillMaxSize().hazeSource(photoMenuHazeState)) {
            MoaMapTitleTopBar(
                title = when (uiState.step) {
                    AddPlaceStep.Search -> "장소 검색"
                    AddPlaceStep.Location -> "지도에서 위치 확인"
                    AddPlaceStep.Form -> "장소 추가"
                },
                onBackClick = onBackClick,
            )

            Box(modifier = Modifier.weight(1f)) {
                val selected = uiState.selected
                when {
                    selected == null -> PlaceSearchContent(
                        query = uiState.query,
                        search = uiState.search,
                        onQueryChange = viewModel::updateQuery,
                        onRetryClick = viewModel::retrySearch,
                        onCandidateClick = viewModel::selectCandidate,
                    )

                    uiState.step == AddPlaceStep.Location -> PlaceLocationContent(
                        candidate = selected,
                        onConfirmClick = viewModel::confirmLocation,
                    )

                    else -> PlaceFormContent(
                        // 단계 제목이 위에 있어 본문 제목은 뺀다(시안).
                        showsTitle = false,
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
                        .padding(
                            start = AddPlaceHorizontalPadding,
                            end = AddPlaceHorizontalPadding,
                            bottom = 16.dp,
                        ),
                )
            }
        }

        // 카메라·갤러리 고르기. 화면 위에 겹쳐 띄우고, 바깥을 누르면 메뉴만 닫는다.
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
                    hazeState = photoMenuHazeState,
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

/** 아래 파란 버튼. 등록 폼(54)과 위치 확인(약 49)이 함께 쓴다. */
@Composable
internal fun SubmitButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
) {
    ShadowedSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
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
