package com.moamap.app.feature.mapdetail

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight

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
    /** 댓글 줄을 꾹 눌렀다. 줄의 화면 위치(root 기준)를 함께 넘긴다. null 이면 메뉴가 없다. */
    onReviewLongPress: ((PlaceReviewUiModel, Rect) -> Unit)? = null,
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
            // 시안의 댓글 줄은 화면 좌우 20 안쪽 폭이다. 꾹 누르면 그 줄이 카드로 떠오른다.
            var bounds by remember { mutableStateOf<Rect?>(null) }
            val longPress = onReviewLongPress?.let { callback -> { bounds?.let { rect -> callback(review, rect) } } }
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .onGloballyPositioned { coordinates -> bounds = coordinates.boundsInRoot() }
                    // 글자뿐 아니라 프로필·시간·사진까지 줄 전체를 받는다(10-30 사용자 결정, iOS 와 다름).
                    .then(
                        if (longPress == null) {
                            Modifier
                        } else {
                            Modifier
                                .pointerInput(review.id) { detectTapGestures(onLongPress = { longPress() }) }
                                .semantics { onLongClick(label = "댓글 메뉴 열기") { longPress(); true } }
                        },
                    ),
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
    /** 흰 입력칸의 자리. 사진 고르기 메뉴가 그 바로 위에 붙는다. */
    onInputPositioned: (LayoutCoordinates) -> Unit = {},
) {
    var reviewText by rememberSaveable(placeId) { mutableStateOf("") }
    val inputEnabled = onSubmitReview != null && !reviews.submitting
    val canSend = inputEnabled && (reviewText.isNotBlank() || photo != null)

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
                            enabled = inputEnabled && photo == null,
                            role = Role.Button,
                            onClick = onAddPhotoClick,
                        )
                        .semantics { contentDescription = "사진 첨부" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        tint = if (inputEnabled && photo == null) {
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
                    .size(24.dp)
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
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * 꾹 누른 댓글. 시안 「리뷰 - Selected」(`5124:69760`): 같은 줄을 회색 바탕·모서리 12 카드로 그리고 아래 선은 뺀다.
 */
@Composable
internal fun HighlightedReviewRow(review: PlaceReviewUiModel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MoaMapTheme.colors.backgroundSecondary),
    ) {
        ReviewRow(review = review, showsDivider = false)
    }
}

/**
 * 댓글 한 줄. 시안 「리뷰」: 높이 80, 안쪽 위아래 16·좌우 12, 프로필 원 28 → 12 → 이름·내용, 시간은 오른쪽 위.
 * 사진이 붙은 댓글은 내용 아래에 사진을 둔다(시안에 없는 자리, 사용자 결정).
 */
@Composable
private fun ReviewRow(review: PlaceReviewUiModel, showsDivider: Boolean = true) {
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

        if (showsDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MoaMapTheme.colors.lineAlternative,
            )
        }
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
