package com.moamap.app.feature.mapdetail

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.common.imagepicker.rememberImagePickerController
import com.moamap.app.core.common.imagepicker.rememberImagePickerState
import com.moamap.app.core.common.upload.ALLOWED_IMAGE_CONTENT_TYPES
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ButtonShadowColor
import com.moamap.app.core.designsystem.component.ImageSourceMenu
import com.moamap.app.core.designsystem.component.MoaMapBackButton
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.component.MoaMapTopBarIconEdgePadding
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.mapdetail.presentation.addplace.PLACE_PHOTO_CACHE_DIRECTORY
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val SwipeActionWidth = 72.dp

/** 시안의 삭제·신고 빨강. 디자인 토큰에 없는 색이라 여기 둔다. */
private val SwipeDangerColor = Color(0xFFD9402F)
private val PlaceDetailTopBarHeight = 58.dp
private val PlacePhotoHeight = 175.dp
private val SheetPhotoHeight = 167.dp
private val PageActionGap = 8.dp
private val CompactActionGap = 4.dp

/** 장소 시트. 시안: 위 모서리 38, 손잡이 칸 16. */
private val SheetCornerRadius = 38.dp
private val SheetGrabberAreaHeight = 16.dp

/** 시트가 버튼 줄에서 끝날 때 아래 여백(시안 「장소 상세 설명」 아래 8). */
private val SheetBottomPadding = 8.dp

/** 사진 없는 시트는 첫 댓글까지 보인다: 버튼 줄 → 12 → 구분선 0.5 → 12 → 댓글 줄 80. */
private val SheetFirstReviewPeek = 12.dp + 0.5.dp + 12.dp + 80.dp

/** 내용이 아주 길어도 시트 윗변이 화면 위에서 이만큼은 내려와 있게 한다. 지도가 조금은 보여야 시트다. */
private const val SheetMinTopFraction = 0.15f

/** 시트를 이만큼 넘게 끌고 놓으면 페이지로·닫힘으로 간다. 머티리얼 바텀시트와 같은 값이다. */
private val SheetDragThreshold = 56.dp
private val SheetVelocityThreshold = 125.dp
private val PlacePhotoShape = RoundedCornerShape(4.dp)
private val PlaceTagShape = RoundedCornerShape(100.dp)
private val PlaceActionShape = RoundedCornerShape(8.dp)
private val ReviewPhotoShape = RoundedCornerShape(8.dp)

/** 이름 줄 오른쪽 끝 외부 링크 버튼. 이름·태그는 이 폭에 간격 8 을 더한 만큼 비켜 선다. */
private val LinkMenuButtonSize = 36.dp
private val LinkMenuButtonClearance = LinkMenuButtonSize + 8.dp
private val LinkMenuItemSize = 44.dp
private val LinkMenuShareSize = 40.dp

/** 시안의 메뉴 간격: 카카오맵·인스타그램 사이 12, 공유하기 앞 21. */
private val LinkMenuItemGap = 12.dp
private val LinkMenuShareGap = 21.dp
private val ReviewAvatarSize = 28.dp
private val ReviewAvatarImageSize = 24.dp
private val ReviewRowMinHeight = 80.dp

/** 후기 자리의 로딩·오류·빈 상태가 함께 쓰는 높이. 상태가 바뀌어도 시트가 튀지 않는다. */
private val ReviewPlaceholderHeight = 140.dp

private val PlaceActionHeight = 44.dp
private val ReviewPhotoSize = 96.dp
private val ReviewDraftPhotoSize = 64.dp

/**
 * 「나만의 지도에 추가」 버튼 상태.
 *
 * 결과 안내를 버튼 아래에 띄운다. 시트 위로는 화면의 스낵바가 보이지 않는다.
 */
@Immutable
internal data class PersonalMapActionUiModel(
    val adding: Boolean = false,
    val message: String? = null,
    val failed: Boolean = false,
)

/** 장소 상세가 보이는 단계. 장소를 누르면 시트로 열리고, 시트를 위로 끌면 페이지로 펼쳐진다. */
internal enum class PlaceDetailStage { Sheet, Page }

/** 끌던 시트를 놓았을 때 갈 곳. [placeSheetDragTarget] 참고. */
internal enum class PlaceSheetTarget { Page, Sheet, Hidden }

/**
 * 시트를 끌다 놓았을 때 갈 곳. 오프셋은 화면 위에서 시트 윗변까지 거리라 작을수록 위다.
 *
 * 빠르게 튕기면 그 방향으로 간다 - 위로는 페이지, 아래로는 시트 자리보다 내려와 있었으면 닫힘(아직
 * 위였으면 시트로 돌아옴). 천천히 놓으면 시트 자리에서 [distanceThreshold] 넘게 벗어났을 때만 움직인다.
 */
internal fun placeSheetDragTarget(
    offset: Float,
    velocity: Float,
    sheetOffset: Float,
    distanceThreshold: Float,
    velocityThreshold: Float,
): PlaceSheetTarget = when {
    velocity <= -velocityThreshold -> PlaceSheetTarget.Page
    velocity >= velocityThreshold -> if (offset < sheetOffset) PlaceSheetTarget.Sheet else PlaceSheetTarget.Hidden
    offset <= sheetOffset - distanceThreshold -> PlaceSheetTarget.Page
    offset >= sheetOffset + distanceThreshold -> PlaceSheetTarget.Hidden
    else -> PlaceSheetTarget.Sheet
}

/**
 * 시트 높이를 재는 자리. 버튼 줄 아래 끝을 시트 윗변 기준으로 잰다.
 *
 * 두 좌표 중 어느 쪽이 먼저 잡혀도 둘 다 있을 때 다시 잰다. 시트와 버튼은 같이 움직여서 끄는 중에도
 * 값이 그대로라 다시 그리지 않는다. 페이지일 때는 배치가 달라 재지 않고 시트 때 값을 둔다.
 */
private class PlaceSheetProbe {
    var measuring = true
    var actionsBottom by mutableFloatStateOf(0f)
        private set

    var panel: LayoutCoordinates? = null
        set(value) {
            field = value
            measure()
        }

    var actions: LayoutCoordinates? = null
        set(value) {
            field = value
            measure()
        }

    private fun measure() {
        if (!measuring) return
        val panel = panel?.takeIf { it.isAttached } ?: return
        val actions = actions?.takeIf { it.isAttached } ?: return
        actionsBottom = panel.localBoundingBoxOf(actions, clipBounds = false).bottom
    }
}

/**
 * 장소 상세. 장소를 누르면 지도 위로 시트 하나가 올라오고(뒤는 어둡게), 위로 끌면 화면을 덮는
 * 페이지로 펼쳐진다. 시안 「10/3」 장소 상세 시트·페이지.
 *
 * 시트 높이는 장소마다 잰다 - 사진이 있으면 버튼 줄까지, 없으면 첫 댓글까지, 댓글이 없는 공식지도는
 * 버튼 줄까지. 사진이 있는 장소는 시트에서만 줄인 배치(태그 옆 주소, 설명·신고하기 없음)를 쓴다.
 * 시트는 화면 높이 그대로이고 아래로 밀어 둔 것이라, 아래쪽 댓글 입력은 페이지가 돼야 보인다.
 *
 * 닫는 길: 시트에서 ←·어두운 곳·아래로 끌기·기기 뒤로 = 장소 목록, × = 처음 들어온 상태. 페이지에서
 * ←·기기 뒤로 = 시트. 이름 오른쪽 버튼은 외부 링크 메뉴(카카오맵·인스타그램·공유하기)를 화면 전체를
 * 어둡게 덮고 그 자리에 펼친다.
 */
@Composable
internal fun PlaceDetailScreen(
    place: PlaceUiModel,
    reviews: PlaceReviewsUiModel,
    /** 장소 목록으로 돌아간다. 시트가 다 내려간 뒤에 부른다. */
    onBackClick: () -> Unit,
    /** 지도 상세에 처음 들어왔을 때의 화면으로 돌아간다. 시트가 다 내려간 뒤에 부른다. */
    onCloseClick: () -> Unit,
    onKakaoMapClick: () -> Unit,
    onInstagramClick: () -> Unit,
    /** 상세를 닫고 지도를 이 장소 마커로 옮긴다. 시트가 다 내려간 뒤에 부른다. */
    onShowOnMapClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRetryReviews: () -> Unit = {},
    /** null 이면 「나만의 지도에 추가」를 띄우지 않는다. 나만의 지도를 보고 있을 때다. */
    personalMapAction: PersonalMapActionUiModel? = null,
    onAddToPersonalMapClick: () -> Unit = {},
    onSubmitReview: ((reviewText: String, photo: Uri?) -> Boolean)? = null,
    onLikeClick: () -> Unit = {},
    onEditReview: (Long) -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onDeleteReview: (Long) -> Unit = {},
    /** false 면 하트·신고하기·댓글을 뺀다. 공식지도다. 나만의 지도 추가·지도 보기·외부 링크는 남는다. */
    showsReactions: Boolean = true,
) {
    var stage by rememberSaveable(place.id) { mutableStateOf(PlaceDetailStage.Sheet) }
    // 외부 링크 메뉴를 연 버튼의 자리(이 화면 기준). null 이면 메뉴가 닫혀 있다.
    var linkMenuAnchor by remember(place.id) { mutableStateOf<Rect?>(null) }
    var screenOrigin by remember { mutableStateOf(Offset.Zero) }
    val probe = remember(place.id) { PlaceSheetProbe() }
    SideEffect { probe.measuring = stage == PlaceDetailStage.Sheet }
    val listState = remember(place.id) { LazyListState() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates -> screenOrigin = coordinates.positionInRoot() },
    ) {
        val screenHeight = constraints.maxHeight.toFloat()
        // 시트 윗변의 자리. 처음엔 화면 아래(닫힘)에서 시작해 높이를 재면 올라온다.
        val offset = remember(place.id) {
            Animatable(if (stage == PlaceDetailStage.Page) 0f else screenHeight)
        }
        // 사진 있는 장소는 시트에서만 줄인 배치다. 시트 높이 계산은 단계와 상관없이 시트 배치 기준이다
        // - 페이지일 땐 시트 때 잰 값을 그대로 쓰고 있어서다.
        val compactSheet = place.photoUrl != null
        val compact = stage == PlaceDetailStage.Sheet && compactSheet
        val sheetOffset = probe.actionsBottom.takeIf { bottom -> bottom > 0f }?.let { actionsBottom ->
            val belowActions = if (!compactSheet && showsReactions) SheetFirstReviewPeek else SheetBottomPadding
            val sheetHeight = actionsBottom + with(density) { belowActions.toPx() }
            (screenHeight - sheetHeight).coerceAtLeast(screenHeight * SheetMinTopFraction)
        }

        // 페이지는 위 끝까지, 시트는 잰 자리로. 페이지에서 시트로 내려오면 목록을 맨 위로 되돌린다.
        LaunchedEffect(stage, sheetOffset) {
            when (stage) {
                PlaceDetailStage.Page -> offset.animateTo(0f)
                PlaceDetailStage.Sheet -> {
                    listState.scrollToItem(0)
                    sheetOffset?.let { target -> offset.animateTo(target) }
                }
            }
        }

        // 시트를 끝까지 내린 뒤 닫는다. 두 번 눌러도 앞의 내리기가 취소돼 한 번만 닫힌다.
        val dismissThen: (() -> Unit) -> Unit = { then ->
            scope.launch {
                offset.animateTo(screenHeight)
                then()
            }
        }
        val onBack = {
            if (stage == PlaceDetailStage.Page) stage = PlaceDetailStage.Sheet else dismissThen(onBackClick)
        }

        // 시트 뒤 지도와 상단 바를 어둡게 덮는다. 시트가 올라온 만큼 짙어지고, 누르면 목록으로.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val sheetTop = sheetOffset ?: screenHeight
                    alpha = ((screenHeight - offset.value) / (screenHeight - sheetTop).coerceAtLeast(1f))
                        .coerceIn(0f, 1f)
                }
                .background(MoaMapPrimitiveColors.TransparentBlack)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClickLabel = "장소 상세 닫기",
                ) { dismissThen(onBackClick) },
        )

        PlaceDetailContent(
            place = place,
            reviews = reviews,
            sheet = stage == PlaceDetailStage.Sheet,
            compact = compact,
            listState = listState,
            onBackClick = onBack,
            onCloseClick = { dismissThen(onCloseClick) },
            onLinkMenuClick = { anchorInRoot -> linkMenuAnchor = anchorInRoot.translate(-screenOrigin) },
            onShowOnMapClick = { dismissThen(onShowOnMapClick) },
            onActionsPositioned = { coordinates -> probe.actions = coordinates },
            onRetryReviews = onRetryReviews,
            personalMapAction = personalMapAction,
            onAddToPersonalMapClick = onAddToPersonalMapClick,
            onSubmitReview = onSubmitReview,
            onLikeClick = onLikeClick,
            onEditReview = onEditReview,
            onCancelEdit = onCancelEdit,
            onDeleteReview = onDeleteReview,
            showsReactions = showsReactions,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offset.value.roundToInt()) }
                .onGloballyPositioned { coordinates -> probe.panel = coordinates }
                // 시트일 땐 위 모서리 38, 위로 끌수록 줄어 페이지에서 0 이 된다.
                .graphicsLayer {
                    val sheetTop = sheetOffset ?: screenHeight
                    val radius = SheetCornerRadius.toPx() *
                        (offset.value / sheetTop.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    shape = RoundedCornerShape(topStart = radius, topEnd = radius)
                    clip = true
                }
                .background(MoaMapTheme.colors.backgroundSecondary)
                // 뒤에 깔린 어두운 막으로 터치가 새지 않게 빈 자리의 탭을 여기서 받는다.
                .pointerInput(Unit) { detectTapGestures() }
                // 시트일 땐 어디를 끌어도 시트가 움직인다(목록은 스크롤을 꺼 둔다). 페이지는 목록이 스크롤된다.
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = stage == PlaceDetailStage.Sheet && sheetOffset != null,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, screenHeight)) }
                    },
                    onDragStopped = { velocity ->
                        val anchor = sheetOffset
                        if (anchor != null) {
                            val target = placeSheetDragTarget(
                                offset = offset.value,
                                velocity = velocity,
                                sheetOffset = anchor,
                                distanceThreshold = with(density) { SheetDragThreshold.toPx() },
                                velocityThreshold = with(density) { SheetVelocityThreshold.toPx() },
                            )
                            when (target) {
                                PlaceSheetTarget.Page -> stage = PlaceDetailStage.Page
                                PlaceSheetTarget.Sheet -> offset.animateTo(anchor)
                                PlaceSheetTarget.Hidden -> {
                                    offset.animateTo(screenHeight)
                                    onBackClick()
                                }
                            }
                        }
                    },
                )
                .then(if (stage == PlaceDetailStage.Page) Modifier.statusBarsPadding() else Modifier),
        )

        linkMenuAnchor?.let { anchor ->
            PlaceLinkMenu(
                anchor = anchor,
                showsInstagram = place.instagramUrl != null,
                onKakaoMapClick = {
                    linkMenuAnchor = null
                    onKakaoMapClick()
                },
                onInstagramClick = {
                    linkMenuAnchor = null
                    onInstagramClick()
                },
                onDismiss = { linkMenuAnchor = null },
            )
        }

        // 상세보다 나중에 생겨 지도 화면의 뒤로가기보다 먼저 받는다. 나중에 둔 메뉴 쪽이 가장 먼저다.
        BackHandler(onBack = onBack)
        BackHandler(enabled = linkMenuAnchor != null) { linkMenuAnchor = null }
    }
}

internal fun likeIconRes(liked: Boolean): Int = if (liked) {
    R.drawable.ic_favorite_filled
} else {
    R.drawable.ic_favorite_outline
}

internal fun trySubmitReview(
    reviewText: String,
    photo: Uri?,
    onSubmitReview: ((reviewText: String, photo: Uri?) -> Boolean)?,
): Boolean = onSubmitReview?.invoke(reviewText, photo) == true

/**
 * 상세 내용. 상단 바와 입력창은 목록과 함께 스크롤되지 않고 위아래에 붙는다(시안).
 *
 * [sheet] 면 위에 손잡이와 ←·× 줄을 두고 목록 스크롤을 끈다(끌면 시트가 움직인다). 아니면 페이지라
 * ←만 둔다. [compact] 는 사진 있는 장소의 시트 배치다.
 *
 * 카메라·갤러리 고르기는 이 안에 겹쳐 띄운다. Popup 으로 띄우면 화면 밖에 그려져, 바깥을
 * 눌렀을 때 상세까지 함께 닫힌다.
 */
@Composable
private fun PlaceDetailContent(
    place: PlaceUiModel,
    reviews: PlaceReviewsUiModel,
    onBackClick: () -> Unit,
    /** 외부 링크 버튼의 자리(화면 루트 기준)를 넘긴다. 메뉴가 그 자리에 펼쳐진다. */
    onLinkMenuClick: (Rect) -> Unit,
    onShowOnMapClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        // 서버가 받지 않는 형식은 갤러리에서부터 보이지 않게 한다.
        mimeTypes = ALLOWED_IMAGE_CONTENT_TYPES.toTypedArray(),
        onImageSelected = { uri -> reviewPhoto = uri },
    )

    // 시트도 화면 높이 그대로라 높이를 제한하지 않는다. 목록이 남은 자리를 다 쓴다.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .dismissKeyboardOnBackgroundTap(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (sheet) {
                PlaceSheetTopBar(onBackClick = onBackClick, onCloseClick = onCloseClick)
            } else {
                PlaceDetailTopBar(onBackClick = onBackClick)
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
                            onShowOnMapClick = onShowOnMapClick,
                            buttonGap = buttonGap,
                            modifier = modifier.onGloballyPositioned(onActionsPositioned),
                        )
                    }
                    // 블록 사이 간격은 시안 그대로 12씩이다.
                    if (compact) {
                        PlaceCompactHeader(
                            place = place,
                            onLikeClick = onLikeClick,
                            onLinkMenuClick = onLinkMenuClick,
                            showsReactions = showsReactions,
                            actions = { actions(Modifier, CompactActionGap) },
                        )
                    } else {
                        PlaceHeader(
                            place = place,
                            onLikeClick = onLikeClick,
                            onLinkMenuClick = onLinkMenuClick,
                            showsReactions = showsReactions,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        actions(Modifier.padding(horizontal = 20.dp), PageActionGap)
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
            Box(
                modifier = Modifier
                    .matchParentSize()
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

private fun androidx.compose.foundation.lazy.LazyListScope.reviewItems(
    reviews: PlaceReviewsUiModel,
    onRetryReviews: () -> Unit,
    swipeEnabled: Boolean = false,
    openReviewId: Long? = null,
    onOpenChange: (Long, Boolean) -> Unit = { _, _ -> },
    onEditClick: (Long) -> Unit = {},
    onDeleteClick: (Long) -> Unit = {},
) {
    val loadErrorMessage = reviews.loadErrorMessage
    when {
        reviews.loading -> item { ReviewPlaceholder { CircularProgressIndicator() } }

        loadErrorMessage != null -> item {
            ReviewPlaceholder {
                ReviewLoadError(message = loadErrorMessage, onRetryClick = onRetryReviews)
            }
        }

        reviews.items.isEmpty() -> item {
            ReviewPlaceholder {
                Text(
                    text = "아직 댓글이 없어요",
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                )
            }
        }

        else -> items(
            items = reviews.items,
            key = PlaceReviewUiModel::id,
        ) { review ->
            // 시안 `1841:11219`·`1841:11391`: 내 댓글은 수정·삭제, 남의 댓글은 신고.
            // 신고는 사유를 정하기 전이라 아직 누를 수 없다.
            val actions = when {
                !swipeEnabled -> emptyList()
                review.mine -> listOf(
                    SwipeAction("수정", MoaMapPrimitiveColors.Gray200) { onEditClick(review.id) },
                    SwipeAction("삭제", SwipeDangerColor) { onDeleteClick(review.id) },
                )
                else -> listOf(SwipeAction("신고", SwipeDangerColor, onClick = null))
            }
            // 시안의 댓글 줄은 화면 좌우 20 안쪽 폭이다. 밀어서 드러나는 버튼도 그 안에 선다.
            SwipeRevealRow(
                actions = actions,
                open = openReviewId == review.id,
                onOpenChange = { open -> onOpenChange(review.id, open) },
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                ReviewRow(review = review)
            }
        }
    }
}

@Composable
private fun ReviewPlaceholder(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ReviewPlaceholderHeight),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ReviewLoadError(message: String, onRetryClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MoaMapPrimitiveColors.Blue500,
            onClick = onRetryClick,
        ) {
            Text(
                text = "다시 시도",
                style = MoaMapTheme.typography.button2,
                color = MoaMapTheme.colors.textWhite,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** 맨 위의 `←`·`×` 줄. 장소 추가 화면과 장소 시트가 같이 쓴다. */
@Composable
internal fun BackCloseControls(
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    // 장소 추가 시안은 위아래 여백 12 + 아이콘 32, 장소 시트 시안은 GNB 58 이다.
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

/** 장소 상세 페이지 상단 바. 시안 GNB 58 에 뒤로가기만 있다. */
@Composable
private fun PlaceDetailTopBar(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
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
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(width = 36.dp, height = 5.dp)
                    // 시안 #CCCCCC. 다른 시트 손잡이와 같은 토큰을 쓴다.
                    .background(color = MoaMapPrimitiveColors.Gray100, shape = PlaceTagShape),
            )
        }
        BackCloseControls(
            onBackClick = onBackClick,
            onCloseClick = onCloseClick,
            height = PlaceDetailTopBarHeight,
        )
    }
}

/**
 * 장소 정보. 시안 「장소 상세 설명」(10/3 장소 상세 페이지·사진 없는 시트).
 *
 * 이름·태그·위치 → 사진(있을 때만) → 설명·하트·신고하기 순서다. 사진이 없는 장소는 사진 칸만
 * 빠지고 나머지 배치는 같다. 외부 링크 버튼은 이름 줄 오른쪽 끝에 겹쳐 선다.
 */
@Composable
private fun PlaceHeader(
    place: PlaceUiModel,
    onLikeClick: () -> Unit,
    onLinkMenuClick: (Rect) -> Unit,
    showsReactions: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlaceName(name = place.name)
                if (place.tags.isNotEmpty()) {
                    PlaceTagRow(
                        tags = place.tags,
                        modifier = Modifier.padding(end = LinkMenuButtonClearance),
                    )
                }
                if (place.address.isNotBlank()) PlaceLocation(address = place.address)
            }

            PlaceLinkMenuButton(
                onClick = onLinkMenuClick,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        place.photoUrl?.let { url -> PlacePhoto(url = url, height = PlacePhotoHeight) }

        val showsDescription = place.description.isNotBlank()
        if (showsDescription || showsReactions) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showsDescription) {
                    Text(
                        text = place.description,
                        style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                        color = MoaMapTheme.colors.textAlternative,
                    )
                }
                if (showsReactions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaceLikeButton(place = place, onClick = onLikeClick)
                        // 신고는 아직 기능이 없어 누르지 않는다(사용자 결정) - 시안의 밑줄 글자만 둔다.
                        Text(
                            text = "신고하기",
                            style = MoaMapTheme.typography.body2.copy(textDecoration = TextDecoration.Underline),
                            color = MoaMapTheme.colors.textAlternative,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 사진 있는 장소의 시트 배치. 시안 「10/3」 시트(사진 있음).
 *
 * 이름 → 태그 · 주소 한 줄 → 하트 → 사진(167) → 버튼 줄(간격 4). 설명·신고하기·위치 아이콘은 없다.
 * 위로 끌어 페이지가 되면 [PlaceHeader] 배치로 바뀐다.
 */
@Composable
private fun PlaceCompactHeader(
    place: PlaceUiModel,
    onLikeClick: () -> Unit,
    onLinkMenuClick: (Rect) -> Unit,
    showsReactions: Boolean,
    actions: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlaceName(name = place.name)
                PlaceTagAddressLine(
                    tags = place.tags,
                    address = place.address,
                    modifier = Modifier.padding(end = LinkMenuButtonClearance),
                )
                if (showsReactions) PlaceLikeButton(place = place, onClick = onLikeClick)
            }

            PlaceLinkMenuButton(
                onClick = onLinkMenuClick,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        place.photoUrl?.let { url -> PlacePhoto(url = url, height = SheetPhotoHeight) }
        actions()
    }
}

@Composable
private fun PlaceName(name: String) {
    Text(
        text = name,
        style = MoaMapTheme.typography.subtitle1.withDesignLineHeight(),
        color = MoaMapTheme.colors.textNormal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(end = LinkMenuButtonClearance),
    )
}

/** 등록할 때 단 태그. 이 줄이 넘치면 다음 줄로 내린다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceTagRow(tags: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag -> PlaceTagChip(tag = tag) }
    }
}

/**
 * 시트의 「태그 · 주소」 한 줄. 넘치면 주소부터 말줄임(사용자 결정), 태그만으로 넘치면 넘친 칩은
 * 잘린다. 태그가 없으면 「·」 없이 주소만 둔다.
 */
@Composable
private fun PlaceTagAddressLine(tags: List<String>, address: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tags.forEach { tag -> PlaceTagChip(tag = tag) }
        if (address.isNotBlank()) {
            Text(
                text = if (tags.isEmpty()) address else "· $address",
                style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                color = MoaMapTheme.colors.textAlternative,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // 칩을 먼저 놓고 남은 폭만 쓴다.
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun PlaceTagChip(tag: String) {
    Text(
        text = tag,
        style = MoaMapTheme.typography.caption0,
        color = MoaMapPrimitiveColors.Yellow900,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(color = MoaMapPrimitiveColors.Yellow50, shape = PlaceTagShape)
            .border(width = 1.dp, color = MoaMapPrimitiveColors.Yellow500, shape = PlaceTagShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun PlaceLocation(address: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_location),
            contentDescription = null,
            tint = MoaMapTheme.colors.textAlternative,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = address,
            style = MoaMapTheme.typography.body2.withDesignLineHeight(),
            color = MoaMapTheme.colors.textAlternative,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 장소 사진 첫 장. 폭 가득, 페이지 175·시트 167. */
@Composable
private fun PlacePhoto(url: String, height: Dp) {
    val placeholder = painterResource(R.drawable.img_photo_placeholder)
    AsyncImage(
        model = url,
        contentDescription = "장소 사진",
        placeholder = placeholder,
        error = placeholder,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(PlacePhotoShape),
    )
}

/** 하트 28 + 수. 누르면 바로 바뀌고 서버로 확정한다. */
@Composable
private fun PlaceLikeButton(place: PlaceUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (place.liked) "하트 취소하기" else "하트 누르기"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(likeIconRes(place.liked)),
            contentDescription = null,
            tint = if (place.liked) MoaMapTheme.colors.statusAlert else MoaMapPrimitiveColors.Gray100,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = place.likeCount.toString(),
            style = MoaMapTheme.typography.caption0,
            color = MoaMapTheme.colors.textAlternative,
        )
    }
}

/**
 * 외부 링크 메뉴를 여는 원 버튼(36). 누르는 칸은 48 이고 그림은 그 가운데라, 칸을 바깥으로 6 씩
 * 내밀어 그림이 시안 자리(이름 줄 오른쪽 끝)에 선다.
 */
@Composable
private fun PlaceLinkMenuButton(
    onClick: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 같은 좌표계 객체가 계속 들어와 다시 그리게 하지 않는다. 자리는 누를 때 읽는다.
    var imageCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier = modifier
            .offset(x = 6.dp, y = (-6).dp)
            .size(48.dp)
            .clickable(role = Role.Button) {
                imageCoordinates?.takeIf { it.isAttached }?.let { coordinates -> onClick(coordinates.boundsInRoot()) }
            }
            .semantics { contentDescription = "외부 링크 열기" },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_place_link_menu),
            contentDescription = null,
            modifier = Modifier
                .size(LinkMenuButtonSize)
                .onGloballyPositioned { coordinates -> imageCoordinates = coordinates },
        )
    }
}

/**
 * 외부 링크 메뉴. 시안 「상세지도/장소/장소선택」의 메뉴 모양: 화면 전체를 75% 검정으로 덮고, 버튼
 * 자리에서 아래로 카카오맵 → 인스타그램(인스타그램에서 가져온 장소만) → 공유하기를 세운다.
 * 오른쪽 끝은 버튼과 맞추고, 첫 아이콘은 버튼과 세로 가운데를 맞춘다. 어두운 곳을 누르면 닫힌다.
 */
@Composable
private fun PlaceLinkMenu(
    anchor: Rect,
    showsInstagram: Boolean,
    onKakaoMapClick: () -> Unit,
    onInstagramClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoaMapPrimitiveColors.TransparentBlack)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        Column(
            modifier = Modifier.offset {
                val itemSize = LinkMenuItemSize.toPx()
                IntOffset(
                    x = (anchor.right - itemSize).roundToInt(),
                    y = (anchor.center.y - itemSize / 2).roundToInt(),
                )
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinkMenuItem(
                imageRes = R.drawable.img_link_kakao_map,
                label = "카카오맵에서 보기",
                onClick = onKakaoMapClick,
            )
            if (showsInstagram) {
                Spacer(modifier = Modifier.height(LinkMenuItemGap))
                LinkMenuItem(
                    imageRes = R.drawable.img_link_instagram,
                    label = "인스타그램 게시물 보기",
                    onClick = onInstagramClick,
                )
            }
            Spacer(modifier = Modifier.height(LinkMenuShareGap))
            // 공유는 아직 기능이 없다(사용자 결정). 눌러도 메뉴가 닫히지 않게 탭만 받아 둔다.
            Box(
                modifier = Modifier
                    .size(LinkMenuShareSize)
                    .clip(CircleShape)
                    .background(MoaMapPrimitiveColors.Gray200)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .semantics { contentDescription = "공유하기" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = null,
                    tint = MoaMapPrimitiveColors.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun LinkMenuItem(imageRes: Int, label: String, onClick: () -> Unit) {
    Image(
        painter = painterResource(imageRes),
        contentDescription = label,
        modifier = Modifier
            .size(LinkMenuItemSize)
            .clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun PlaceActions(
    personalMapAction: PersonalMapActionUiModel?,
    onAddToPersonalMapClick: () -> Unit,
    onShowOnMapClick: () -> Unit,
    /** 두 버튼 사이. 시안은 페이지 8, 사진 있는 시트 4. */
    buttonGap: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(buttonGap),
        ) {
            if (personalMapAction != null) {
                PlaceActionButton(
                    label = "나만의 지도에 추가",
                    iconRes = R.drawable.ic_add,
                    containerColor = MoaMapPrimitiveColors.Blue500,
                    contentColor = MoaMapPrimitiveColors.White,
                    loading = personalMapAction.adding,
                    onClick = onAddToPersonalMapClick,
                    modifier = Modifier.weight(1f),
                )
            }
            PlaceActionButton(
                label = "지도 보기",
                iconRes = R.drawable.ic_map,
                containerColor = MoaMapPrimitiveColors.Gray200,
                contentColor = MoaMapPrimitiveColors.White,
                loading = false,
                onClick = onShowOnMapClick,
                modifier = Modifier.weight(1f),
            )
        }

        personalMapAction?.message?.let { message ->
            Text(
                text = message,
                style = MoaMapTheme.typography.caption0,
                // 성공은 다른 안내 문구와 같은 회색, 실패만 빨강으로 눈에 띄게 한다.
                color = if (personalMapAction.failed) {
                    MoaMapTheme.colors.statusAlert
                } else {
                    MoaMapTheme.colors.textAssistive
                },
            )
        }
    }
}

@Composable
private fun PlaceActionButton(
    label: String,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.height(PlaceActionHeight),
        shape = PlaceActionShape,
        color = containerColor,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ButtonShadowColor,
        onClick = if (loading) null else onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = label,
                style = MoaMapTheme.typography.button2,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

/**
 * 후기 입력. 별점 없이 글과 사진 한 장을 남긴다.
 *
 * 사진은 시트가 들고 있다가([photo]) 서버가 받아들인 뒤에 비운다. 보내자마자 지우면 실패했을 때
 * 고른 사진이 날아간다.
 */
@Composable
private fun ReviewComposer(
    placeId: Long,
    reviews: PlaceReviewsUiModel,
    photo: Uri?,
    onAddPhotoClick: () -> Unit,
    onRemovePhotoClick: () -> Unit,
    onPhotoSubmitted: () -> Unit,
    onSubmitReview: ((reviewText: String, photo: Uri?) -> Boolean)?,
    /** 고치고 있는 내 댓글. 있으면 입력창이 그 글로 채워지고 보내기가 수정이 된다. */
    editing: PlaceReviewUiModel? = null,
    onCancelEdit: () -> Unit = {},
) {
    var reviewText by rememberSaveable(placeId) { mutableStateOf("") }
    val inputEnabled = onSubmitReview != null && !reviews.submitting
    // 사진이 있는 댓글은 글을 비워도 고칠 수 있다. 사진은 그대로 남는다.
    val canSend = inputEnabled &&
        (reviewText.isNotBlank() || photo != null || editing?.photoUrl != null)

    // 고치기 시작하면 원래 글을 채우고, 끝나면(취소·삭제) 비운다. 고른 사진은 뺀다 - 사진은 고치지 않는다.
    var prefilledId by remember(placeId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(editing?.id) {
        val editingId = editing?.id
        if (editing != null) {
            reviewText = editing.message
            onRemovePhotoClick()
        } else if (prefilledId != null) {
            reviewText = ""
        }
        prefilledId = editingId
    }

    LaunchedEffect(reviews.submittedCount) {
        if (reviews.submittedCount > 0) {
            reviewText = ""
            onPhotoSubmitted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (editing != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "댓글 수정 중",
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapTheme.colors.textAlternative,
                )
                Text(
                    text = "취소",
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapTheme.colors.textNormal,
                    modifier = Modifier.clickable(enabled = !reviews.submitting, onClick = onCancelEdit),
                )
            }
        }

        if (photo != null) {
            ReviewDraftPhoto(
                photo = photo,
                removable = inputEnabled,
                onRemoveClick = onRemovePhotoClick,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 시안은 「검색창」 모양(흰 바탕·모서리 12·카드 그림자)에 돋보기 대신 + 다. ShadowedSurface 기본값이
            // 그 모양이다. 그림자는 내용 뒤에 따로 깔아야 한다 - blur 를 입력창에 걸면 글자까지 흐려진다.
            ShadowedSurface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
            ) {
                Row(
                    // + 는 누르는 칸 32 가운데 20 이라 왼쪽 10 이면 그림이 시안처럼 끝에서 16 에 선다.
                    modifier = Modifier.padding(start = 10.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            // 사진은 한 장만 받는다. 이미 골랐으면 지우고 다시 고른다.
                            .clickable(
                                enabled = inputEnabled && photo == null && editing == null,
                                role = Role.Button,
                                onClick = onAddPhotoClick,
                            )
                            .semantics { contentDescription = "사진 첨부" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            tint = if (inputEnabled && photo == null && editing == null) {
                                MoaMapTheme.colors.textAssistive
                            } else {
                                MoaMapTheme.colors.textDisable
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    BasicTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        modifier = Modifier.weight(1f),
                        enabled = inputEnabled,
                        singleLine = true,
                        textStyle = MoaMapTheme.typography.body2.copy(
                            color = MoaMapTheme.colors.textNormal,
                        ),
                        cursorBrush = SolidColor(MoaMapPrimitiveColors.Blue500),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (reviewText.isEmpty()) {
                                    Text(
                                        text = if (onSubmitReview == null) {
                                            "지도에 참여하면 댓글을 남길 수 있어요"
                                        } else {
                                            "이 장소에 대한 경험을 공유해주세요"
                                        },
                                        style = MoaMapTheme.typography.body2,
                                        color = MoaMapTheme.colors.textAssistive,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) MoaMapPrimitiveColors.Blue500 else MoaMapPrimitiveColors.Gray200,
                    )
                    .clickable(
                        enabled = canSend,
                        role = Role.Button,
                        // 입력은 여기서 비우지 않는다. 서버가 받아들였는지는 아직 모른다.
                        onClick = { trySubmitReview(reviewText, photo, onSubmitReview) },
                    )
                    .semantics { contentDescription = "댓글 보내기" },
                contentAlignment = Alignment.Center,
            ) {
                if (reviews.submitting) {
                    CircularProgressIndicator(
                        color = MoaMapPrimitiveColors.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    // 시안 보내기 원 40 안의 아이콘 32. 앱 ic_send 는 같은 그림을 24 틀로 줄인 것이다.
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = null,
                        tint = MoaMapPrimitiveColors.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        reviews.submitErrorMessage?.let { message ->
            Text(
                text = message,
                style = MoaMapTheme.typography.caption0,
                color = MoaMapTheme.colors.statusAlert,
            )
        }
    }
}

/** 보내기 전의 첨부 사진. 오른쪽 위를 눌러 뺀다. */
@Composable
private fun ReviewDraftPhoto(
    photo: Uri,
    removable: Boolean,
    onRemoveClick: () -> Unit,
) {
    Box(modifier = Modifier.size(ReviewDraftPhotoSize)) {
        AsyncImage(
            model = photo,
            contentDescription = "첨부한 사진",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(ReviewPhotoShape)
                .background(MoaMapPrimitiveColors.Gray50),
        )
        if (removable) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MoaMapPrimitiveColors.Black.copy(alpha = 0.6f))
                    .clickable(role = Role.Button, onClick = onRemoveClick)
                    .semantics { contentDescription = "첨부한 사진 빼기" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    tint = MoaMapPrimitiveColors.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** 밀었을 때 드러나는 버튼. [onClick] 이 null 이면 보이기만 하고 눌리지 않는다. */
private class SwipeAction(val label: String, val color: Color, val onClick: (() -> Unit)?)

/**
 * 왼쪽으로 밀면 오른쪽에 [actions] 가 드러나는 줄. 버튼은 폭 72, 줄 높이 전체다.
 *
 * 절반 넘게 밀고 놓으면 열리고, 아니면 닫힌다. 열린 채로 줄을 누르면 닫힌다.
 * 버튼이 없으면 밀리지 않는다.
 */
@Composable
private fun SwipeRevealRow(
    actions: List<SwipeAction>,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (actions.isEmpty()) {
        Box(modifier = modifier) { content() }
        return
    }

    val revealPx = with(LocalDensity.current) { (SwipeActionWidth * actions.size).toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(open, revealPx) { offset.animateTo(if (open) -revealPx else 0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
        ) {
            actions.forEach { action ->
                val onClick = action.onClick
                Box(
                    modifier = Modifier
                        .width(SwipeActionWidth)
                        .fillMaxHeight()
                        .background(action.color)
                        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = action.label,
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapTheme.colors.textWhite,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                // 밑의 버튼이 비치지 않게 화면 배경으로 덮는다.
                .background(MoaMapTheme.colors.backgroundSecondary)
                .clickable(enabled = open) { onOpenChange(false) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(-revealPx, 0f)) }
                    },
                    onDragStopped = {
                        val shouldOpen = offset.value < -revealPx / 2
                        onOpenChange(shouldOpen)
                        offset.animateTo(if (shouldOpen) -revealPx else 0f)
                    },
                ),
        ) {
            content()
        }
    }
}

/**
 * 댓글 한 줄. 시안 「리뷰」: 높이 80, 안쪽 위아래 16·좌우 12, 프로필 원 28 → 12 → 이름·내용, 시간은 오른쪽 위.
 * 사진이 붙은 댓글은 내용 아래에 사진을 둔다(시안에 없는 자리, 사용자 결정).
 */
@Composable
private fun ReviewRow(review: PlaceReviewUiModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ReviewRowMinHeight)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReviewAvatar(imageUrl = review.userImageUrl)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = review.userName,
                        style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                        color = MoaMapTheme.colors.textNormal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = review.relativeTime,
                        style = MoaMapTheme.typography.caption0.withDesignLineHeight(),
                        color = MoaMapTheme.colors.textAssistive,
                    )
                }
                // 사진만 남기고 글은 비워 둘 수 있다. 그때 빈 줄이 끼지 않게 통째로 뺀다.
                if (review.message.isNotBlank()) {
                    Text(
                        text = review.message,
                        style = MoaMapTheme.typography.body1.withDesignLineHeight(),
                        color = MoaMapTheme.colors.textNormal,
                    )
                }
                review.photoUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "댓글 사진",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(ReviewPhotoSize)
                            .clip(ReviewPhotoShape)
                            .background(MoaMapPrimitiveColors.Gray50),
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MoaMapTheme.colors.lineAlternative,
        )
    }
}

/** 작성자 프로필. 흰 원 28(테두리 1) 안에 사진 24, 사진이 없으면 기본 사진. */
@Composable
private fun ReviewAvatar(imageUrl: String?) {
    Box(
        modifier = Modifier
            .size(ReviewAvatarSize)
            .background(color = MoaMapPrimitiveColors.White, shape = CircleShape)
            .border(width = 1.dp, color = MoaMapTheme.colors.lineNormal, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PhotoThumbnail(
            imageUrl = imageUrl,
            size = ReviewAvatarImageSize,
            modifier = Modifier.clip(CircleShape),
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
                onLinkMenuClick = {},
                onShowOnMapClick = {},
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
                onLinkMenuClick = {},
                onShowOnMapClick = {},
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
                onLinkMenuClick = {},
                onShowOnMapClick = {},
                sheet = true,
                compact = true,
                personalMapAction = PersonalMapActionUiModel(),
                onSubmitReview = { _, _ -> true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceLinkMenuPreview() {
    MoaMapTheme {
        PlaceLinkMenu(
            anchor = Rect(left = 337f, top = 110f, right = 373f, bottom = 146f),
            showsInstagram = true,
            onKakaoMapClick = {},
            onInstagramClick = {},
            onDismiss = {},
        )
    }
}
