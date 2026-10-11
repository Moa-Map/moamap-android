package com.moamap.app.feature.mapdetail

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.common.imagepicker.rememberImagePickerController
import com.moamap.app.core.common.imagepicker.rememberImagePickerState
import com.moamap.app.core.designsystem.component.ImageSourceMenu
import com.moamap.app.core.designsystem.component.MoaMapBackButton
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.component.MoaMapSheetGrabber
import com.moamap.app.core.designsystem.component.MoaMapTopBarIconEdgePadding
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.presentation.addplace.PLACE_PHOTO_CACHE_DIRECTORY
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt

private val PlaceDetailTopBarHeight = 58.dp

/** 장소 시트 손잡이 칸. 시안 16. */
private val SheetGrabberAreaHeight = 16.dp

/** 사진 고르기 메뉴 아래 끝과 댓글 입력칸 사이(시안). */
private val PhotoMenuGapAboveInput = 4.dp
private val PageActionGap = 8.dp
private val CompactActionGap = 4.dp

/**
 * 상세 내용. 상단 바와 입력창은 목록과 함께 스크롤되지 않고 위아래에 붙는다(시안).
 *
 * [sheet] 면 위에 손잡이와 ←·× 줄을 두고 목록 스크롤을 끈다(끌면 시트가 움직인다). 아니면 페이지라
 * ←만 둔다. [compact] 는 사진 있는 장소의 시트 배치다. 시트가 페이지가 될 때 상단 줄과 배치는 겹쳐
 * 바뀌고 높이도 부드럽게 따라간다.
 *
 * 카메라·갤러리 고르기는 이 안에 겹쳐 띄운다. Popup 으로 띄우면 화면 밖에 그려져, 바깥을
 * 눌렀을 때 상세까지 함께 닫힌다.
 */
@Composable
internal fun PlaceDetailContent(
    place: PlaceUiModel,
    reviews: PlaceReviewsUiModel,
    onBackClick: () -> Unit,
    onKakaoMapClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 인스타그램에서 가져온 장소에만 이름 오른쪽에 버튼을 두고, 누르면 이걸 부른다. */
    onInstagramClick: () -> Unit = {},
    sheet: Boolean = false,
    compact: Boolean = false,
    onCloseClick: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    /** 버튼 줄의 자리. 시트 높이를 여기에 맞춘다. */
    onActionsPositioned: (LayoutCoordinates) -> Unit = {},
    onRetryReviews: () -> Unit = {},
    personalMapAction: PersonalMapActionUiModel? = null,
    onAddToPersonalMapClick: () -> Unit = {},
    onSubmitReview: ((reviewText: String, photo: Uri?) -> Boolean)? = null,
    onLikeClick: () -> Unit = {},
    onEditReview: (Long) -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onDeleteReview: (Long) -> Unit = {},
    showsReactions: Boolean = true,
) {
    var reviewPhoto by rememberSaveable(place.id) { mutableStateOf<Uri?>(null) }
    // 옆으로 밀어 버튼이 드러난 댓글. 한 번에 한 줄만 연다.
    var openReviewId by remember(place.id) { mutableStateOf<Long?>(null) }
    // 삭제를 확인받는 중인 댓글.
    var deleteTargetId by remember(place.id) { mutableStateOf<Long?>(null) }
    val pickerState = rememberImagePickerState()
    val pickerController = rememberImagePickerController(
        state = pickerState,
        cacheDirectoryName = PLACE_PHOTO_CACHE_DIRECTORY,
        fileNamePrefix = "review",
        galleryTitle = "댓글 사진",
        onImageSelected = { uri -> reviewPhoto = uri },
    )
    // 사진 고르기 메뉴는 이 화면을 흐려 바탕으로 깔고, 댓글 입력칸 바로 위에 붙는다(시안).
    val photoMenuHazeState = rememberHazeState()
    var contentOrigin by remember { mutableStateOf(Offset.Zero) }
    var composerInputBounds by remember { mutableStateOf<Rect?>(null) }

    // 시트도 화면 높이 그대로라 높이를 제한하지 않는다. 목록이 남은 자리를 다 쓴다.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> contentOrigin = coordinates.positionInRoot() }
            .dismissKeyboardOnBackgroundTap(),
    ) {
        Column(modifier = Modifier.fillMaxSize().hazeSource(photoMenuHazeState)) {
            Box(modifier = Modifier.animateContentSize()) {
                Crossfade(targetState = sheet, label = "placeDetailTopBar") { isSheet ->
                    if (isSheet) {
                        PlaceSheetTopBar(onBackClick = onBackClick, onCloseClick = onCloseClick)
                    } else {
                        PlaceDetailTopBar(onBackClick = onBackClick)
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                userScrollEnabled = !sheet,
            ) {
                item {
                    val actions = @Composable { modifier: Modifier, buttonGap: Dp ->
                        PlaceActions(
                            personalMapAction = personalMapAction,
                            onAddToPersonalMapClick = onAddToPersonalMapClick,
                            onShowOnMapClick = onKakaoMapClick,
                            buttonGap = buttonGap,
                            modifier = modifier.onGloballyPositioned(onActionsPositioned),
                        )
                    }
                    val instagramClick = onInstagramClick.takeIf { place.instagramUrl != null }
                    // 블록 사이 간격은 시안 그대로 12씩이다. 사진 있는 장소가 페이지가 되면 배치가 겹쳐 바뀐다.
                    Box(modifier = Modifier.animateContentSize()) {
                        Crossfade(targetState = compact, label = "placeDetailHeader") { isCompact ->
                            if (isCompact) {
                                PlaceCompactHeader(
                                    place = place,
                                    onLikeClick = onLikeClick,
                                    onInstagramClick = instagramClick,
                                    showsReactions = showsReactions,
                                    actions = { actions(Modifier, CompactActionGap) },
                                )
                            } else {
                                Column {
                                    PlaceHeader(
                                        place = place,
                                        onLikeClick = onLikeClick,
                                        onInstagramClick = instagramClick,
                                        showsReactions = showsReactions,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    actions(Modifier.padding(horizontal = 20.dp), PageActionGap)
                                }
                            }
                        }
                    }
                    // 구분선은 댓글과 나누는 줄이라 댓글이 없으면 같이 뺀다.
                    if (showsReactions) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MoaMapTheme.colors.lineNormal,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (showsReactions) {
                    reviewItems(
                        reviews = reviews,
                        onRetryReviews = onRetryReviews,
                        // 수정·삭제·신고는 모두 지도 멤버만 할 수 있다. 참여하지 않았으면 밀리지 않는다.
                        swipeEnabled = onSubmitReview != null,
                        openReviewId = openReviewId,
                        onOpenChange = { id, open ->
                            openReviewId = if (open) id else openReviewId.takeUnless { it == id }
                        },
                        onEditClick = { id ->
                            openReviewId = null
                            onEditReview(id)
                        },
                        onDeleteClick = { id ->
                            openReviewId = null
                            deleteTargetId = id
                        },
                    )
                }
            }

            if (showsReactions) {
                ReviewComposer(
                    placeId = place.id,
                    reviews = reviews,
                    photo = reviewPhoto,
                    onAddPhotoClick = pickerState::showSourceMenu,
                    onRemovePhotoClick = { reviewPhoto = null },
                    onPhotoSubmitted = { reviewPhoto = null },
                    onSubmitReview = onSubmitReview,
                    editing = reviews.items.firstOrNull { item -> item.id == reviews.editingReviewId },
                    onCancelEdit = onCancelEdit,
                    onInputPositioned = { coordinates -> composerInputBounds = coordinates.boundsInRoot() },
                )
            }
        }

        deleteTargetId?.let { id ->
            MoaMapConfirmDialog(
                title = "댓글을 삭제하시겠습니까?",
                message = "삭제한 댓글은 되돌릴 수 없습니다",
                confirmText = "삭제",
                onConfirm = {
                    deleteTargetId = null
                    onDeleteReview(id)
                },
                onDismissRequest = { deleteTargetId = null },
            )
        }

        if (pickerState.isSourceMenuVisible) {
            // 입력칸 자리는 화면 기준이라 이 화면 기준으로 옮긴다. 키보드가 올라와 입력칸이 움직여도 따라간다.
            val anchor = composerInputBounds?.translate(-contentOrigin)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures { pickerState.dismissSourceMenu() }
                    },
                // 입력칸을 아직 못 쟀으면 예전처럼 가운데에 띄운다.
                contentAlignment = if (anchor == null) Alignment.Center else Alignment.TopStart,
            ) {
                ImageSourceMenu(
                    onCameraClick = pickerController::requestCamera,
                    onGalleryClick = pickerController::requestGallery,
                    hazeState = photoMenuHazeState,
                    modifier = if (anchor == null) {
                        Modifier
                    } else {
                        // 메뉴 왼쪽 끝은 입력칸 왼쪽 끝, 아래 끝은 입력칸 위 [PhotoMenuGapAboveInput](시안).
                        Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                            layout(placeable.width, placeable.height) {
                                placeable.place(
                                    x = anchor.left.roundToInt(),
                                    y = (anchor.top - PhotoMenuGapAboveInput.toPx()).roundToInt() - placeable.height,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

/** 맨 위의 `←`·`×` 줄. 장소 시트가 쓴다(장소 추가는 10-09 시안부터 제목 바에 `←` 만). */
@Composable
internal fun BackCloseControls(
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    // 장소 시트 시안은 GNB 58 이다.
    height: Dp = 56.dp,
) {
    // 두 아이콘 모두 48 칸 가운데에 선다. 바깥 12 + 안쪽 8 이라 양 끝에서 20 이다.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = MoaMapTopBarIconEdgePadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoaMapBackButton(onClick = onBackClick)

        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onCloseClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "닫기",
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

/** 장소 상세 페이지 상단 바. 시안 GNB 58 에 뒤로가기만 있다. 페이지는 화면 맨 위라 상태 표시줄만큼 내린다. */
@Composable
private fun PlaceDetailTopBar(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(PlaceDetailTopBarHeight),
    ) {
        MoaMapBackButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = MoaMapTopBarIconEdgePadding),
        )
    }
}

/** 장소 시트 맨 위. 손잡이(위 5, 36×5) 아래 GNB 58 에 ←·×. */
@Composable
private fun PlaceSheetTopBar(onBackClick: () -> Unit, onCloseClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SheetGrabberAreaHeight),
            contentAlignment = Alignment.TopCenter,
        ) {
            MoaMapSheetGrabber(modifier = Modifier.padding(top = 5.dp), width = 36.dp)
        }
        BackCloseControls(
            onBackClick = onBackClick,
            onCloseClick = onCloseClick,
            height = PlaceDetailTopBarHeight,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceDetailPreview() {
    MoaMapTheme {
        Surface(color = MoaMapTheme.colors.backgroundSecondary) {
            PlaceDetailContent(
                // 프리뷰는 사진을 받지 않아 사진 칸에 기본 사진이 뜬다.
                place = SamplePlaces.first().copy(photoUrl = "preview"),
                reviews = PlaceReviewsUiModel(items = SamplePlaceReviews),
                onBackClick = {},
                onKakaoMapClick = {},
                personalMapAction = PersonalMapActionUiModel(message = "나만의 지도에 추가했어요"),
                onSubmitReview = { _, _ -> true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 사진이 없는 장소. 나만의 지도를 보고 있어 지도 보기만 남고, 참여 전이라 입력도 막힌다. */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceDetailNoPhotoPreview() {
    MoaMapTheme {
        Surface(color = MoaMapTheme.colors.backgroundSecondary) {
            PlaceDetailContent(
                place = SamplePlaces[1],
                reviews = PlaceReviewsUiModel(items = SamplePlaceReviews.take(2)),
                onBackClick = {},
                onKakaoMapClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 사진 있는 장소의 시트 배치(손잡이·←·×, 태그 · 주소 한 줄, 사진 167, 버튼 간격 4). */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceDetailCompactSheetPreview() {
    MoaMapTheme {
        Surface(color = MoaMapTheme.colors.backgroundSecondary) {
            PlaceDetailContent(
                place = SamplePlaces.first().copy(photoUrl = "preview"),
                reviews = PlaceReviewsUiModel(items = SamplePlaceReviews),
                onBackClick = {},
                onKakaoMapClick = {},
                sheet = true,
                compact = true,
                personalMapAction = PersonalMapActionUiModel(),
                onSubmitReview = { _, _ -> true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
