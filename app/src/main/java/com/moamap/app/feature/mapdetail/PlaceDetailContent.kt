package com.moamap.app.feature.mapdetail

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import com.moamap.app.core.designsystem.component.MoaMapToast
import com.moamap.app.core.designsystem.component.MoaMapTopBarIconEdgePadding
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.ReportReason
import com.moamap.app.feature.mapdetail.presentation.addplace.PLACE_PHOTO_CACHE_DIRECTORY
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt

private val PlaceDetailTopBarHeight = 58.dp

/** 장소 시트 손잡이 칸. 시안 16. */
private val SheetGrabberAreaHeight = 16.dp

/** 사진 고르기 메뉴 아래 끝과 댓글 입력칸 사이(시안). */
private val PhotoMenuGapAboveInput = 4.dp

/** 꾹 누른 댓글 카드 위 끝과 메뉴 아래 끝 사이(시안 `5124:69760`). */
private val ReviewMenuGap = 2.dp

/** 꾹 눌렀을 때 화면을 덮는 검정 30%(시안). */
private val ReviewMenuDim = MoaMapPrimitiveColors.Black.copy(alpha = 0.3f)

/** 토스트 아래 끝과 댓글 입력칸 사이. 시안은 토스트가 입력 줄 위 끝에 거의 붙어 있다. */
private val ToastGapAboveInput = 4.dp
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
    onDeleteReview: (Long) -> Unit = {},
    /** 신고 팝업에서 사유를 고른 뒤 부른다. */
    onReportReview: (Long, ReportReason, String) -> Unit = { _, _, _ -> },
    /** 차단 팝업에서 확인한 뒤 부른다. */
    onBlockReview: (Long) -> Unit = {},
    showsReactions: Boolean = true,
    /** null 이 아니면 페이지 상단 바에 ⋮ 메뉴(수정하기·삭제하기)를 둔다. 고치거나 지울 수 있는 장소다. */
    onEditPlaceClick: (() -> Unit)? = null,
    /** 메뉴의 삭제하기를 팝업에서 한 번 더 확인받은 뒤 부른다. */
    onDeletePlace: () -> Unit = {},
    /** 한 번 띄우는 완료 토스트. 장소 정보를 고친 뒤다. 댓글 입력 줄 바로 위에 뜬다(시안). */
    toastMessage: String? = null,
    onToastShown: () -> Unit = {},
) {
    var reviewPhoto by rememberSaveable(place.id) { mutableStateOf<Uri?>(null) }
    // 메뉴는 화면을 돌리면 닫혀도 된다. 시트로 내려가면 메뉴 아이콘이 없어 함께 닫는다.
    var placeMenuVisible by remember(place.id) { mutableStateOf(false) }
    LaunchedEffect(sheet) { if (sheet) placeMenuVisible = false }
    var deletePlaceConfirmVisible by rememberSaveable(place.id) { mutableStateOf(false) }
    // 꾹 눌러 메뉴를 띄운 댓글과 그 줄의 화면 위치. 시트로 내려가면 닫는다 - 메뉴는 페이지에서만 뜬다.
    var menuTarget by remember(place.id) { mutableStateOf<Pair<PlaceReviewUiModel, Rect>?>(null) }
    LaunchedEffect(sheet) { if (sheet) menuTarget = null }
    // 메뉴에서 고른 뒤 팝업으로 확인받는 중인 댓글.
    var deleteTarget by remember(place.id) { mutableStateOf<PlaceReviewUiModel?>(null) }
    var blockTarget by remember(place.id) { mutableStateOf<PlaceReviewUiModel?>(null) }
    var reportTarget by remember(place.id) { mutableStateOf<PlaceReviewUiModel?>(null) }
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
                        PlaceDetailTopBar(
                            onBackClick = onBackClick,
                            onMenuClick = onEditPlaceClick?.let { { placeMenuVisible = true } },
                        )
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
                        // 시트를 끝까지 올린 페이지에서만 띄운다(iOS 와 같음). 시트에서는 끌면 시트가 움직인다.
                        onReviewLongPress = if (!sheet && reviews.menuEnabled) {
                            { review, bounds -> menuTarget = review to bounds }
                        } else {
                            null
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
                    onInputPositioned = { coordinates -> composerInputBounds = coordinates.boundsInRoot() },
                )
            }
        }

        menuTarget?.let { (review, rootBounds) ->
            val bounds = rootBounds.translate(-contentOrigin)
            val density = LocalDensity.current
            // 덮개를 누르면 닫는다. 덮개는 상단 바·입력 줄까지 다 덮는다(시안).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(ReviewMenuDim)
                    .pointerInput(Unit) { detectTapGestures { menuTarget = null } },
            )
            HighlightedReviewRow(
                review = review,
                modifier = Modifier
                    .width(with(density) { bounds.width.toDp() })
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        layout(placeable.width, placeable.height) {
                            placeable.place(bounds.left.roundToInt(), bounds.top.roundToInt())
                        }
                    },
            )
            ReviewActionMenu(
                mine = review.mine,
                onDeleteClick = {
                    menuTarget = null
                    deleteTarget = review
                },
                onBlockClick = {
                    menuTarget = null
                    blockTarget = review
                },
                onReportClick = {
                    menuTarget = null
                    reportTarget = review
                },
                hazeState = photoMenuHazeState,
                // 메뉴 왼쪽 끝은 댓글 카드 왼쪽 끝, 아래 끝은 카드 위 2(시안). 위에 자리가 없으면 카드 아래에 둔다.
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    layout(placeable.width, placeable.height) {
                        val above = bounds.top - ReviewMenuGap.toPx() - placeable.height
                        val y = if (above >= 0f) above else bounds.bottom + ReviewMenuGap.toPx()
                        placeable.place(bounds.left.roundToInt(), y.roundToInt())
                    }
                },
            )
            BackHandler { menuTarget = null }
        }

        deleteTarget?.let { review ->
            MoaMapConfirmDialog(
                title = "댓글을 삭제할까요?",
                message = "삭제한 댓글은 복구할 수 없어요",
                confirmText = "삭제하기",
                dismissText = "취소",
                dismissColor = MoaMapPrimitiveColors.Gray200,
                onConfirm = {
                    deleteTarget = null
                    onDeleteReview(review.id)
                },
                onDismissRequest = { deleteTarget = null },
            )
        }

        blockTarget?.let { review ->
            // 이름을 모르는 작성자는 화면에 「이름 없는 사용자」로 보이니 그대로 묻는다.
            MoaMapConfirmDialog(
                title = review.userName,
                titleSuffix = "님을 차단할까요?",
                message = "모든 지도에서 이 사람의 댓글, 게시물이\n보이지 않고 알림이 오지 않아요\n설정에서 차단을 해제할 수 있어요",
                confirmText = "차단하기",
                dismissText = "취소",
                dismissColor = MoaMapPrimitiveColors.Gray200,
                onConfirm = {
                    blockTarget = null
                    onBlockReview(review.id)
                },
                onDismissRequest = { blockTarget = null },
            )
        }

        reportTarget?.let { review ->
            ReportDialog(
                title = "댓글 신고하기",
                onSubmit = { reason, detail ->
                    reportTarget = null
                    onReportReview(review.id, reason, detail)
                },
                onDismissRequest = { reportTarget = null },
            )
        }

        if (deletePlaceConfirmVisible) {
            // 시안이 없어 댓글 삭제 팝업과 같은 말투로 둔다(이슈 #376).
            MoaMapConfirmDialog(
                title = "장소를 삭제할까요?",
                message = "삭제한 장소는 되돌릴 수 없어요",
                confirmText = "삭제",
                onConfirm = {
                    deletePlaceConfirmVisible = false
                    onDeletePlace()
                },
                onDismissRequest = { deletePlaceConfirmVisible = false },
            )
        }

        val onEditPlace = onEditPlaceClick
        if (placeMenuVisible && onEditPlace != null) {
            // 메뉴 밖을 누르면 닫는다.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures { placeMenuVisible = false }
                    },
            )
            PlaceDetailMenu(
                onEditClick = {
                    placeMenuVisible = false
                    onEditPlace()
                },
                onDeleteClick = {
                    placeMenuVisible = false
                    deletePlaceConfirmVisible = true
                },
                hazeState = photoMenuHazeState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = MapDetailMenuTop, end = MapDetailMenuEnd),
            )
            BackHandler { placeMenuVisible = false }
        }

        // 댓글 입력 줄 바로 위(시안). 입력 줄이 없는 장소(공식지도)는 아래에서 20 위다.
        val toastAnchor = composerInputBounds?.takeIf { showsReactions }?.translate(-contentOrigin)
        MoaMapToast(
            message = toastMessage,
            onShown = onToastShown,
            modifier = if (toastAnchor == null) {
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            } else {
                Modifier
                    .align(Alignment.TopCenter)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        layout(placeable.width, placeable.height) {
                            val bottom = toastAnchor.top - ToastGapAboveInput.toPx()
                            placeable.place(x = 0, y = bottom.roundToInt() - placeable.height)
                        }
                    }
            },
        )

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

/**
 * 장소 상세 페이지 상단 바. 시안 GNB 58 에 뒤로가기, 고치거나 지울 수 있는 장소면 오른쪽 끝 20 에 메뉴(32).
 * 페이지는 화면 맨 위라 상태 표시줄만큼 내린다.
 */
@Composable
private fun PlaceDetailTopBar(onBackClick: () -> Unit, onMenuClick: (() -> Unit)?) {
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
        onMenuClick?.let { onClick ->
            Icon(
                painter = painterResource(R.drawable.ic_menu),
                contentDescription = "장소 메뉴",
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = MapDetailMenuEnd)
                    .clickable(onClick = onClick)
                    .size(32.dp),
            )
        }
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
