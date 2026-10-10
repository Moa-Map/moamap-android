package com.moamap.app.feature.mapdetail

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
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
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapSheetGrabber
import com.moamap.app.core.designsystem.component.MoaMapTopBarIconEdgePadding
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.mapdetail.presentation.addplace.PLACE_PHOTO_CACHE_DIRECTORY
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val SwipeActionWidth = 72.dp

/** 시안의 삭제·신고 빨강. 디자인 토큰에 없는 색이라 여기 둔다. */
private val SwipeDangerColor = Color(0xFFD9402F)
private val PlaceDetailTopBarHeight = 58.dp
private val PlacePhotoHeight = 175.dp
private val SheetPhotoHeight = 167.dp

/** 사진 고르기 메뉴 아래 끝과 댓글 입력칸 사이(시안). */
private val PhotoMenuGapAboveInput = 4.dp
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

/** 시트 자리보다 이만큼 넘게 끌어내리고 놓으면 닫힌다. 머티리얼 바텀시트와 같은 값이다. */
private val SheetDragThreshold = 56.dp

/** 시트 자리에서 화면 위까지 가는 길의 마지막 이 비율 안으로 끌어 올리고 놓으면 페이지가 된다(「거의 다」). */
private const val PageSnapFraction = 0.2f
/**
 * 놓을 때 이보다 빠르면(초당) 튕긴 것으로 본다. 머티리얼 기본값 125 는 천천히 끌다 놓는 손끝 속도로도
 * 넘어 「천천히 올리면 멈춘다」(사용자 결정)가 안 돼서, 일부러 튕길 때만 넘게 높게 잡는다.
 */
private val SheetVelocityThreshold = 1000.dp
private val PlacePhotoShape = RoundedCornerShape(4.dp)
private val PlaceTagShape = RoundedCornerShape(100.dp)
private val PlaceActionShape = RoundedCornerShape(8.dp)
private val ReviewPhotoShape = RoundedCornerShape(8.dp)

/** 이름 줄 오른쪽 끝 인스타그램 버튼. 버튼이 있으면 이름·태그는 이 폭에 간격 8 을 더한 만큼 비켜 선다. */
private val InstagramButtonSize = 36.dp
private val InstagramButtonClearance = InstagramButtonSize + 8.dp
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

/** 장소 상세가 보이는 단계. 장소를 누르면 시트로 열리고, 시트를 거의 끝까지 끌어 올리면 페이지가 된다. */
internal enum class PlaceDetailStage { Sheet, Page }

/** 끌던 시트를 놓았을 때 갈 곳. [placeSheetDragTarget] 참고. */
internal enum class PlaceSheetTarget { Page, Sheet, Hidden, Stay }

/**
 * 시트를 끌다 놓았을 때 갈 곳. 오프셋은 화면 위에서 시트 윗변까지 거리라 작을수록 위다.
 *
 * 빠르게 튕기면 그 방향으로 간다 - 위로는 페이지, 아래로는 시트 자리보다 내려와 있었으면 닫힘(아직
 * 위였으면 시트로 돌아옴). 천천히 놓으면 [pageLine] 위(거의 끝)는 페이지, 시트 자리와 그 사이는 놓은
 * 자리에 멈춤(사용자 결정), 시트 자리보다 아래는 [distanceThreshold] 넘게 내렸으면 닫힘·아니면 시트로.
 */
internal fun placeSheetDragTarget(
    offset: Float,
    velocity: Float,
    sheetOffset: Float,
    pageLine: Float,
    distanceThreshold: Float,
    velocityThreshold: Float,
): PlaceSheetTarget = when {
    velocity <= -velocityThreshold -> PlaceSheetTarget.Page
    velocity >= velocityThreshold -> if (offset < sheetOffset) PlaceSheetTarget.Sheet else PlaceSheetTarget.Hidden
    offset <= pageLine -> PlaceSheetTarget.Page
    offset >= sheetOffset + distanceThreshold -> PlaceSheetTarget.Hidden
    offset > sheetOffset -> PlaceSheetTarget.Sheet
    else -> PlaceSheetTarget.Stay
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
 * 장소 상세. 장소를 누르면 지도 위로 시트 하나가 올라오고(위는 지도가 그대로 보인다), 끌어 올린 만큼 아래 내용이
 * 드러난다. 중간에 놓으면 그 자리에 멈추고, 거의 끝까지 올리면 화면을 덮는 페이지가 된다. 시안
 * 「10/3」 장소 상세 시트·페이지.
 *
 * 시트 높이는 장소마다 잰다 - 사진이 있으면 버튼 줄까지, 없으면 첫 댓글까지, 댓글이 없는 공식지도는
 * 버튼 줄까지. 사진이 있는 장소는 시트에서만 줄인 배치(태그 옆 주소, 설명·신고하기 없음)를 쓰고,
 * 페이지가 되면 겹쳐 바뀐다. 시트는 화면 높이 그대로이고 아래로 밀어 둔 것이라, 아래쪽 댓글 입력은
 * 페이지가 돼야 보인다.
 *
 * 닫는 길: 시트·페이지의 ←·기기 뒤로, 시트 위 빈 지도 누르기([closeSignal])·아래로 끌기 = 장소 목록,
 * × = 처음 들어온 상태.
 */
@Composable
internal fun PlaceDetailScreen(
    place: PlaceUiModel,
    reviews: PlaceReviewsUiModel,
    /** 장소 목록으로 돌아간다. 시트가 다 내려간 뒤에 부른다. */
    onBackClick: () -> Unit,
    /** 지도 상세에 처음 들어왔을 때의 화면으로 돌아간다. 시트가 다 내려간 뒤에 부른다. */
    onCloseClick: () -> Unit,
    /** 「지도 보기」. 카카오맵에서 이 장소를 연다. */
    onKakaoMapClick: () -> Unit,
    /** 인스타그램에서 가져온 장소의 원본 게시물을 연다. 그런 장소에만 버튼이 있다. */
    onInstagramClick: () -> Unit,
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
    /** false 면 하트·신고하기·댓글을 뺀다. 공식지도다. 나만의 지도 추가·지도 보기는 남는다. */
    showsReactions: Boolean = true,
    /** 시트 자리가 정해졌을 때. 화면 아래에서 시트 윗변까지의 높이(px)를 준다. */
    onSheetPlaced: (sheetHeightPx: Float) -> Unit = {},
    /** 올리면 시트가 내려가며 닫힌다(장소 목록으로). 시트 위 빈 지도를 눌렀을 때다. */
    closeSignal: Int = 0,
) {
    var stage by rememberSaveable(place.id) { mutableStateOf(PlaceDetailStage.Sheet) }
    val probe = remember(place.id) { PlaceSheetProbe() }
    SideEffect { probe.measuring = stage == PlaceDetailStage.Sheet }
    val listState = remember(place.id) { LazyListState() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
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
        // 이 선보다 위로 올리고 놓으면 페이지가 된다(시트 자리에서 끝까지의 마지막 20%).
        val pageLine = sheetOffset?.let { anchor ->
            (anchor * PageSnapFraction).coerceAtMost(anchor - with(density) { SheetDragThreshold.toPx() })
        }

        // 페이지는 위 끝까지, 시트는 잰 자리로. 놓은 자리에 멈춘 시트는 단계가 그대로라 건드리지 않는다.
        LaunchedEffect(stage, sheetOffset) {
            when (stage) {
                PlaceDetailStage.Page -> offset.animateTo(0f)
                PlaceDetailStage.Sheet -> sheetOffset?.let { target -> offset.animateTo(target) }
            }
        }

        // 시트를 끝까지 내린 뒤 닫는다. 두 번 눌러도 앞의 내리기가 취소돼 한 번만 닫힌다.
        val dismissThen: (() -> Unit) -> Unit = { then ->
            scope.launch {
                offset.animateTo(screenHeight)
                then()
            }
        }
        val onBack = { dismissThen(onBackClick) }

        // 시트 자리가 정해지면 밖에 알린다. 지도가 그 위 빈 곳으로 장소를 옮긴다. 장소가 바뀌면 높이가
        // 같아도 다시 알린다 - 다른 장소로 옮겨야 한다.
        val currentOnSheetPlaced by rememberUpdatedState(onSheetPlaced)
        LaunchedEffect(place.id, sheetOffset) {
            sheetOffset?.let { top -> currentOnSheetPlaced(screenHeight - top) }
        }

        // 시트 위 빈 지도를 누르면 오른다. 열릴 때의 값과 다르면 내려가며 닫는다.
        val closeSignalAtOpen = remember { closeSignal }
        LaunchedEffect(closeSignal) {
            if (closeSignal != closeSignalAtOpen) onBack()
        }

        PlaceDetailContent(
            place = place,
            reviews = reviews,
            sheet = stage == PlaceDetailStage.Sheet,
            compact = compact,
            listState = listState,
            onBackClick = onBack,
            onCloseClick = { dismissThen(onCloseClick) },
            onInstagramClick = onInstagramClick,
            onKakaoMapClick = onKakaoMapClick,
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
                // 위 모서리 38. 거의 끝까지 올라와 페이지가 될 자리에 들어서면 줄어 페이지에서 0 이 된다.
                .graphicsLayer {
                    val line = pageLine ?: screenHeight
                    val radius = SheetCornerRadius.toPx() * (offset.value / line.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    shape = RoundedCornerShape(topStart = radius, topEnd = radius)
                    clip = true
                }
                .background(MoaMapTheme.colors.backgroundSecondary)
                // 뒤에 깔린 지도로 터치가 새지 않게 빈 자리의 탭을 여기서 받는다. 새면 빈 지도 탭이 돼 닫힌다.
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
                        val line = pageLine
                        if (anchor != null && line != null) {
                            val target = placeSheetDragTarget(
                                offset = offset.value,
                                velocity = velocity,
                                sheetOffset = anchor,
                                pageLine = line,
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
                                PlaceSheetTarget.Stay -> Unit
                            }
                        }
                    },
                ),
        )

        // 상세보다 나중에 생겨 지도 화면의 뒤로가기보다 먼저 받는다.
        BackHandler(onBack = onBack)
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
 * ←만 둔다. [compact] 는 사진 있는 장소의 시트 배치다. 시트가 페이지가 될 때 상단 줄과 배치는 겹쳐
 * 바뀌고 높이도 부드럽게 따라간다.
 *
 * 카메라·갤러리 고르기는 이 안에 겹쳐 띄운다. Popup 으로 띄우면 화면 밖에 그려져, 바깥을
 * 눌렀을 때 상세까지 함께 닫힌다.
 */
@Composable
private fun PlaceDetailContent(
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
        // 서버가 받지 않는 형식은 갤러리에서부터 보이지 않게 한다.
        mimeTypes = ALLOWED_IMAGE_CONTENT_TYPES.toTypedArray(),
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
                MoaMapErrorNotice(message = loadErrorMessage, onRetryClick = onRetryReviews)
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

/**
 * 장소 정보. 시안 「장소 상세 설명」(10/3 장소 상세 페이지·사진 없는 시트).
 *
 * 이름·태그·위치 → 사진(있을 때만) → 설명·하트·신고하기 순서다. 사진이 없는 장소는 사진 칸만
 * 빠지고 나머지 배치는 같다. 인스타그램에서 가져온 장소는 이름 줄 오른쪽 끝에 인스타그램 버튼이
 * 겹쳐 선다([onInstagramClick] 가 null 이면 없다).
 */
@Composable
private fun PlaceHeader(
    place: PlaceUiModel,
    onLikeClick: () -> Unit,
    onInstagramClick: (() -> Unit)?,
    showsReactions: Boolean,
) {
    val clearance = if (onInstagramClick != null) InstagramButtonClearance else 0.dp
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
                PlaceName(name = place.name, endClearance = clearance)
                if (place.tags.isNotEmpty()) {
                    PlaceTagRow(
                        tags = place.tags,
                        modifier = Modifier.padding(end = clearance),
                    )
                }
                if (place.address.isNotBlank()) PlaceLocation(address = place.address)
            }

            onInstagramClick?.let { onClick ->
                PlaceInstagramButton(onClick = onClick, modifier = Modifier.align(Alignment.TopEnd))
            }
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
 * 사진 있는 장소의 시트 배치. 시안 「10/3」 시트(사진 있음·사진 있음 + 인스타 버튼).
 *
 * 이름 → 태그 · 주소 한 줄 → 하트 → 사진(167) → 버튼 줄(간격 4). 설명·신고하기·위치 아이콘은 없다.
 * 거의 끝까지 끌어 올려 페이지가 되면 [PlaceHeader] 배치로 겹쳐 바뀐다.
 */
@Composable
private fun PlaceCompactHeader(
    place: PlaceUiModel,
    onLikeClick: () -> Unit,
    onInstagramClick: (() -> Unit)?,
    showsReactions: Boolean,
    actions: @Composable () -> Unit,
) {
    val clearance = if (onInstagramClick != null) InstagramButtonClearance else 0.dp
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
                PlaceName(name = place.name, endClearance = clearance)
                PlaceTagAddressLine(
                    tags = place.tags,
                    address = place.address,
                    modifier = Modifier.padding(end = clearance),
                )
                if (showsReactions) PlaceLikeButton(place = place, onClick = onLikeClick)
            }

            onInstagramClick?.let { onClick ->
                PlaceInstagramButton(onClick = onClick, modifier = Modifier.align(Alignment.TopEnd))
            }
        }

        place.photoUrl?.let { url -> PlacePhoto(url = url, height = SheetPhotoHeight) }
        actions()
    }
}

@Composable
private fun PlaceName(name: String, endClearance: Dp) {
    Text(
        text = name,
        style = MoaMapTheme.typography.subtitle1.withDesignLineHeight(),
        color = MoaMapTheme.colors.textNormal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(end = endClearance),
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
 * 인스타그램 원본 게시물로 가는 원 버튼(36). 인스타그램 링크로 가져온 장소에만 둔다(사용자 결정).
 * 누르는 칸은 48 이고 그림은 그 가운데라, 칸을 바깥으로 6 씩 내밀어 그림이 시안 자리(이름 줄 오른쪽
 * 끝)에 선다.
 */
@Composable
private fun PlaceInstagramButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .offset(x = 6.dp, y = (-6).dp)
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "인스타그램 게시물 보기" },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_place_instagram_link),
            contentDescription = null,
            modifier = Modifier.size(InstagramButtonSize),
        )
    }
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
    /** 흰 입력칸의 자리. 사진 고르기 메뉴가 그 바로 위에 붙는다. */
    onInputPositioned: (LayoutCoordinates) -> Unit = {},
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

        // 시안은 「검색창」 모양에 돋보기 대신 + 다.
        CommentInputRow(
            text = reviewText,
            onTextChange = { reviewText = it },
            placeholder = if (onSubmitReview == null) {
                "지도에 참여하면 댓글을 남길 수 있어요"
            } else {
                "이 장소에 대한 경험을 공유해주세요"
            },
            enabled = inputEnabled,
            canSend = canSend,
            sending = reviews.submitting,
            onSendClick = { trySubmitReview(reviewText, photo, onSubmitReview) },
            onInputPositioned = onInputPositioned,
            leading = {
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
            },
        )

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
        // 테두리는 바깥 28 원이 그린다. 안쪽까지 그리면 두 겹이 된다.
        PhotoThumbnail(
            imageUrl = imageUrl,
            size = ReviewAvatarImageSize,
            shape = CircleShape,
            bordered = false,
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
