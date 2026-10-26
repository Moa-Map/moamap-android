package com.moamap.app.feature.mapdetail

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** 장소 시트 위 모서리. 시안 38. */
private val SheetCornerRadius = 38.dp

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
    /** null 이 아니면 페이지 상단 바에 ⋮ 메뉴(수정하기·삭제하기)를 둔다. */
    onEditPlaceClick: (() -> Unit)? = null,
    /** 삭제를 팝업에서 확인받은 뒤 부른다. */
    onDeletePlace: () -> Unit = {},
    toastMessage: String? = null,
    onToastShown: () -> Unit = {},
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
            onEditPlaceClick = onEditPlaceClick,
            onDeletePlace = onDeletePlace,
            toastMessage = toastMessage,
            onToastShown = onToastShown,
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
