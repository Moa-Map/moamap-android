package com.moamap.app.feature.mapdetail.presentation.posts

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.MoaMapCenteredNotice
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapLoadingIndicator
import com.moamap.app.core.designsystem.component.MoaMapToast
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.modifier.dismissKeyboardOnBackgroundTap
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.mapdetail.CommentInputRow
import com.moamap.app.feature.mapdetail.ReportDialog
import com.moamap.app.feature.mapdetail.ReviewActionMenu
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import com.moamap.app.feature.mapdetail.domain.model.MapPostComment
import com.moamap.app.feature.mapdetail.domain.model.ReportReason
import kotlin.math.roundToInt

/** 시안 「상세 지도/로그 - 새 로그 공유」(`5157:28914`) 바탕. 지도 위 막이 아니라 따로 선 검은 화면이다. */
private val PostDetailBackground = Color(0xFF121212)

/** 뒤로가기 원: 시안 #4A4F52 의 70%. */
private val BackButtonColor = MoaMapPrimitiveColors.Gray500.copy(alpha = 0.7f)

/** 꾹 눌렀을 때 화면을 덮는 검정 30%(시안). */
private val CommentMenuDim = Color.Black.copy(alpha = 0.3f)

/** 꾹 누른 말풍선과 메뉴 사이(시안 `5157:29048`). */
private val CommentMenuGap = 8.dp
private val CommentBubbleShape = RoundedCornerShape(16.dp)
private val CommentAvatarSize = 18.dp

/** 말풍선이 줄 끝까지 차면 내 것·남의 것이 구별되지 않는다. 화면 폭(353)의 8할 쯤에서 줄을 바꾼다. */
private val CommentBubbleMaxWidth = 280.dp

/**
 * 시안(`5157:29048`): 뒤로가기 원은 상태 표시줄 아래 3 에 40, 카드는 상태 표시줄 아래 13 에서 시작한다.
 * 원이 카드 왼쪽 위 모서리에 겹쳐 뜬다.
 */
private val BackButtonTop = 3.dp
private val BackButtonSize = 40.dp
private val ListTop = 13.dp

/** 아래 입력 줄(위 그라데이션 70 + 입력칸 44 + 아래 30)에 마지막 댓글이 가리지 않게 비워 두는 높이. */
private val ListBottom = 150.dp

/** 시안 입력 줄 뒤 그라데이션: 위는 투명, 가운데쯤 검정 20%, 아래 10%. */
private val InputBarGradient = Brush.verticalGradient(
    0f to Color.Transparent,
    0.46f to Color.Black.copy(alpha = 0.2f),
    1f to Color.Black.copy(alpha = 0.1f),
)

private const val UNKNOWN_AUTHOR = "알 수 없는 사용자"


/**
 * 로그 탭 게시물 상세. 시안 `2052:18510`(카드는 컴포넌트 「프라이빗 로그 - 확대」).
 *
 * 지도 상세를 덮는 검은 화면(`#121212`, 시안 `5157:28914`)에 크게 펼친 카드, 그 아래 댓글, 맨 아래 입력 줄을 둔다.
 * 닫기는 왼쪽 위 ←·기기 뒤로가기(지도 상세가 받는다). 바탕을 눌러 닫던 것은 뺐다 - 막이 아니라 화면이다.
 *
 * 말풍선을 꾹 누르면 덮개 위로 그 말풍선이 떠오르고 메뉴(삭제 / 차단·신고)가 붙는다. 메뉴는 말풍선이 화면
 * 위쪽이면 아래에, 아래쪽이면 위에 둔다(10-30 사용자 결정). 남의 댓글은 왼쪽 끝, 내 댓글은 오른쪽 끝을 맞춘다.
 *
 * 시안과 다르게 둔 것: 입력 줄의 사진 + 는 서버가 댓글 사진을 받지 않아 뺐다(10-10 사용자 결정). 입력 줄 뒤
 * 흐림(2.5)과 말풍선 그림자는 검은 바탕 위라 거의 보이지 않아 그리지 않는다.
 *
 * @param canComment 지도 멤버. 아니면 입력칸을 막고 참여 안내를 띄운다(장소 댓글과 같다).
 */
@Composable
internal fun MapPostDetailScreen(
    post: MapPost,
    state: MapPostDetailUiState,
    canComment: Boolean,
    onSendClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteComment: (Long) -> Unit = {},
    onReportComment: (Long, ReportReason, String) -> Unit = { _, _, _ -> },
    onBlockComment: (Long) -> Unit = {},
    onToastShown: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    var commentText by rememberSaveable(post.id) { mutableStateOf("") }
    // 꾹 눌러 메뉴를 띄운 댓글과 그 말풍선의 화면 위치. 팝업으로 확인받는 중인 댓글.
    var menuTarget by remember(post.id) { mutableStateOf<Pair<MapPostComment, Rect>?>(null) }
    var deleteTarget by remember(post.id) { mutableStateOf<MapPostComment?>(null) }
    var blockTarget by remember(post.id) { mutableStateOf<MapPostComment?>(null) }
    var reportTarget by remember(post.id) { mutableStateOf<MapPostComment?>(null) }
    var contentOrigin by remember { mutableStateOf(Offset.Zero) }
    var contentHeight by remember { mutableStateOf(0) }

    // 검은 바탕이라 상태 표시줄 글자를 희게 한다. 닫으면 지도 화면의 색으로 되돌린다.
    val window = LocalActivity.current?.window
    DisposableEffect(window) {
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }

    // 남겼으면 입력칸을 비우고 내 댓글이 보이게 맨 아래로 내린다.
    LaunchedEffect(state.sentCount) {
        if (state.sentCount == 0) return@LaunchedEffect
        commentText = ""
        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
    }

    // 지도 상세 위를 덮는다. 빈 곳을 누르면 키보드를 닫고, 뒤의 지도로 터치가 새지 않게 받는다.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PostDetailBackground)
            .dismissKeyboardOnBackgroundTap()
            .onGloballyPositioned { coordinates ->
                contentOrigin = coordinates.positionInRoot()
                contentHeight = coordinates.size.height
            },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, top = ListTop, end = 20.dp, bottom = ListBottom),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "post") {
                // 시안: 카드와 댓글 사이 16. 목록 간격 4 에 12 를 더한다.
                ExpandedPostCard(post = post, modifier = Modifier.padding(bottom = 12.dp))
            }

            when {
                state.loading -> item(key = "loading") { MoaMapCenteredNotice { MoaMapLoadingIndicator() } }

                state.errorMessage != null -> item(key = "error") {
                    MoaMapCenteredNotice { MoaMapErrorNotice(onDark = true, message = state.errorMessage, onRetryClick = onRetryClick) }
                }

                state.comments.isEmpty() -> item(key = "empty") {
                    MoaMapCenteredNotice {
                        Text(
                            text = "아직 댓글이 없어요",
                            style = MoaMapTheme.typography.body2,
                            color = MoaMapTheme.colors.textWhite,
                        )
                    }
                }

                else -> items(state.comments, key = { comment -> comment.id }) { comment ->
                    PostComment(
                        comment = comment,
                        mine = comment.isMine(state.myId),
                        // 내가 누군지 알아야 내 댓글에 차단·신고를 띄우지 않는다.
                        onLongPress = if (state.myId != null) {
                            { bounds -> menuTarget = comment to bounds }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 14.dp, top = BackButtonTop)
                .size(BackButtonSize)
                .clip(CircleShape)
                .background(BackButtonColor)
                .clickable(role = Role.Button, onClick = onCloseClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "뒤로 가기",
                tint = MoaMapPrimitiveColors.White,
                modifier = Modifier.size(28.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(InputBarGradient)
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, top = 70.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val enabled = canComment && state.commentsReady && !state.sending
            CommentInputRow(
                text = commentText,
                onTextChange = { text -> commentText = text.take(COMMENT_MAX_LENGTH) },
                placeholder = if (canComment) "댓글을 입력해주세요" else "지도에 참여하면 댓글을 남길 수 있어요",
                enabled = enabled,
                canSend = enabled && commentText.isNotBlank(),
                sending = state.sending,
                onSendClick = { onSendClick(commentText) },
            )
            state.sendErrorMessage?.let { message ->
                Text(
                    text = message,
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapTheme.colors.statusAlert,
                )
            }
        }

        // 입력칸 바로 위(시안 「신고가 접수됐어요」). 입력 줄은 아래 30 + 입력칸 44 다.
        MoaMapToast(
            message = state.toast,
            onShown = onToastShown,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 30.dp + 44.dp + 4.dp),
        )

        menuTarget?.let { (comment, rootBounds) ->
            val bounds = rootBounds.translate(-contentOrigin)
            val density = LocalDensity.current
            val mine = comment.isMine(state.myId)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(CommentMenuDim)
                    .pointerInput(Unit) { detectTapGestures { menuTarget = null } },
            )
            CommentBubble(
                text = comment.content,
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
                mine = mine,
                onDeleteClick = {
                    menuTarget = null
                    deleteTarget = comment
                },
                onBlockClick = {
                    menuTarget = null
                    blockTarget = comment
                },
                onReportClick = {
                    menuTarget = null
                    reportTarget = comment
                },
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    layout(placeable.width, placeable.height) {
                        val gap = CommentMenuGap.toPx()
                        val y = if (bounds.center.y < contentHeight / 2f) {
                            bounds.bottom + gap
                        } else {
                            bounds.top - gap - placeable.height
                        }
                        val x = if (mine) bounds.right - placeable.width else bounds.left
                        placeable.place(x.roundToInt(), y.roundToInt())
                    }
                },
            )
            BackHandler { menuTarget = null }
        }

        deleteTarget?.let { comment ->
            MoaMapConfirmDialog(
                title = "댓글을 삭제할까요?",
                message = "삭제한 댓글은 복구할 수 없어요",
                confirmText = "삭제하기",
                dismissText = "취소",
                dismissColor = MoaMapPrimitiveColors.Gray200,
                onConfirm = {
                    deleteTarget = null
                    onDeleteComment(comment.id)
                },
                onDismissRequest = { deleteTarget = null },
            )
        }

        blockTarget?.let { comment ->
            MoaMapConfirmDialog(
                title = comment.authorName ?: UNKNOWN_AUTHOR,
                titleSuffix = "님을 차단할까요?",
                message = "모든 지도에서 이 사람의 댓글, 게시물이\n보이지 않고 알림이 오지 않아요\n설정에서 차단을 해제할 수 있어요",
                confirmText = "차단하기",
                dismissText = "취소",
                dismissColor = MoaMapPrimitiveColors.Gray200,
                onConfirm = {
                    blockTarget = null
                    onBlockComment(comment.id)
                },
                onDismissRequest = { blockTarget = null },
            )
        }

        reportTarget?.let { comment ->
            ReportDialog(
                title = "댓글 신고하기",
                onSubmit = { reason, detail ->
                    reportTarget = null
                    onReportComment(comment.id, reason, detail)
                },
                onDismissRequest = { reportTarget = null },
            )
        }
    }
}

/**
 * 댓글 한 줄. 시안 컴포넌트 「프라이빗 로그 댓글」.
 *
 * 남의 댓글은 왼쪽에 작성자 사진 18·이름(흰 글자)을 위에 두고, 내 댓글은 오른쪽에 말풍선만 둔다.
 * [onLongPress] 는 말풍선(흰 여백 포함)만 받는다 - 이름·옆 빈 곳은 받지 않는다(10-30 사용자 결정).
 */
@Composable
private fun PostComment(comment: MapPostComment, mine: Boolean, onLongPress: ((Rect) -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!mine) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhotoThumbnail(
                    imageUrl = comment.authorImageUrl,
                    size = CommentAvatarSize,
                    shape = CircleShape,
                    bordered = false,
                )
                Text(
                    text = comment.authorName ?: UNKNOWN_AUTHOR,
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapTheme.colors.textWhite,
                )
            }
        }
        var bounds by remember { mutableStateOf<Rect?>(null) }
        val longPress = onLongPress?.let { callback -> { bounds?.let(callback) ?: Unit } }
        CommentBubble(
            text = comment.content,
            modifier = Modifier
                .onGloballyPositioned { coordinates -> bounds = coordinates.boundsInRoot() }
                .then(
                    if (longPress == null) {
                        Modifier
                    } else {
                        Modifier
                            .pointerInput(comment.id) { detectTapGestures(onLongPress = { longPress() }) }
                            .semantics { onLongClick(label = "댓글 메뉴 열기") { longPress(); true } }
                    },
                ),
        )
    }
}

/** 흰 말풍선. 꾹 눌러 떠오른 것도 같은 모양으로 그려 자리가 어긋나지 않게 한다. */
@Composable
private fun CommentBubble(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        // 시안 말풍선은 여백 12 + 줄 높이 21 이다. 줄 높이 여백을 자르면 말풍선이 5 쯤 낮아진다.
        style = MoaMapTheme.typography.body2.withDesignLineHeight(),
        color = MoaMapTheme.colors.textNormal,
        modifier = modifier
            .widthIn(max = CommentBubbleMaxWidth)
            .clip(CommentBubbleShape)
            .background(MoaMapPrimitiveColors.White)
            .padding(12.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, widthDp = 393, heightDp = 852)
@Composable
private fun MapPostDetailScreenPreview() {
    MoaMapTheme {
        MapPostDetailScreen(
            post = SampleMapPosts.first(),
            state = MapPostDetailUiState(
                loading = false,
                myId = 1L,
                comments = listOf(
                    MapPostComment(1L, 2L, "이서연", null, "창가 자리 좋아 보여요!", null),
                    MapPostComment(2L, 1L, null, null, "평일 오전이 한가해요", null),
                    MapPostComment(3L, 3L, null, null, "다음에 같이 가요", null),
                ),
            ),
            canComment = true,
            onSendClick = {},
            onRetryClick = {},
            onCloseClick = {},
        )
    }
}
