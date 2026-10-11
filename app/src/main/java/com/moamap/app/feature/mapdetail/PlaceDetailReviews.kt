package com.moamap.app.feature.mapdetail

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val SwipeActionWidth = 72.dp

/** 시안의 삭제·신고 빨강. 디자인 토큰에 없는 색이라 여기 둔다. */
private val SwipeDangerColor = Color(0xFFD9402F)

private val ReviewPhotoShape = RoundedCornerShape(8.dp)
private val ReviewAvatarSize = 28.dp
private val ReviewAvatarImageSize = 24.dp
private val ReviewRowMinHeight = 80.dp
private val ReviewPhotoSize = 96.dp
private val ReviewDraftPhotoSize = 64.dp

/** 후기 자리의 로딩·오류·빈 상태가 함께 쓰는 높이. 상태가 바뀌어도 시트가 튀지 않는다. */
private val ReviewPlaceholderHeight = 140.dp

internal fun trySubmitReview(
    reviewText: String,
    photo: Uri?,
    onSubmitReview: ((reviewText: String, photo: Uri?) -> Boolean)?,
): Boolean = onSubmitReview?.invoke(reviewText, photo) == true

internal fun androidx.compose.foundation.lazy.LazyListScope.reviewItems(
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

/**
 * 후기 입력. 별점 없이 글과 사진 한 장을 남긴다.
 *
 * 사진은 시트가 들고 있다가([photo]) 서버가 받아들인 뒤에 비운다. 보내자마자 지우면 실패했을 때
 * 고른 사진이 날아간다.
 */
@Composable
internal fun ReviewComposer(
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
