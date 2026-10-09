package com.moamap.app.feature.mapdetail.presentation.posts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.CommentInputRow
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import com.moamap.app.feature.mapdetail.domain.model.MapPostComment

private val CloseButtonColor = Color(0xFF3E3E3F)
private val CommentBubbleShape = RoundedCornerShape(16.dp)
private val CommentAvatarSize = 18.dp

/** 말풍선이 줄 끝까지 차면 내 것·남의 것이 구별되지 않는다. 화면 폭(353)의 8할 쯤에서 줄을 바꾼다. */
private val CommentBubbleMaxWidth = 280.dp

/** 시안: 닫기 원은 상태 표시줄 아래 3 에 40, 카드는 그 아래 12(상태 표시줄 아래 55)에서 시작한다. */
private val CloseButtonTop = 3.dp
private val CloseButtonSize = 40.dp
private val ListTop = 55.dp

/** 아래 입력 줄(위 그라데이션 70 + 입력칸 44 + 아래 30)에 마지막 댓글이 가리지 않게 비워 두는 높이. */
private val ListBottom = 150.dp

/** 시안 입력 줄 뒤 그라데이션: 위는 투명, 가운데쯤 검정 20%, 아래 10%. */
private val InputBarGradient = Brush.verticalGradient(
    0f to Color.Transparent,
    0.46f to Color.Black.copy(alpha = 0.2f),
    1f to Color.Black.copy(alpha = 0.1f),
)

private const val UNKNOWN_AUTHOR = "알 수 없는 사용자"

/** 안의 카드·말풍선·입력 줄을 눌러도 뒤의 검은 막(닫기)까지 탭이 내려가지 않게 받는다. */
private fun Modifier.consumeTaps(): Modifier = pointerInput(Unit) { detectTapGestures() }

/**
 * 로그 탭 게시물 상세. 시안 `2052:18510`(카드는 컴포넌트 「프라이빗 로그 - 확대」).
 *
 * 지도 상세 위에 검은 막(75%)을 덮고 크게 펼친 카드, 그 아래 댓글, 맨 아래 입력 줄을 띄운다.
 * 닫기는 X·검은 막·기기 뒤로가기(지도 상세가 받는다).
 *
 * 시안과 다르게 둔 것: 입력 줄의 사진 + 는 서버가 댓글 사진을 받지 않아 뺐다(10-10 사용자 결정). 입력 줄 뒤
 * 흐림(2.5)과 말풍선 그림자는 검은 막 위라 거의 보이지 않아 그리지 않는다.
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
) {
    val listState = rememberLazyListState()
    var commentText by rememberSaveable(post.id) { mutableStateOf("") }

    // 남겼으면 입력칸을 비우고 내 댓글이 보이게 맨 아래로 내린다.
    LaunchedEffect(state.sentCount) {
        if (state.sentCount == 0) return@LaunchedEffect
        commentText = ""
        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapPrimitiveColors.TransparentBlack)
            // clickable 이 아니라 탭만 받는다. 화면 전체가 버튼으로 읽히면 화면 낭독이 시끄럽다 - 닫기는 X 가 읽힌다.
            .pointerInput(onCloseClick) { detectTapGestures(onTap = { onCloseClick() }) },
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
                ExpandedPostCard(post = post, modifier = Modifier.padding(bottom = 12.dp).consumeTaps())
            }

            when {
                state.loading -> item(key = "loading") { CenteredNotice { Progress() } }

                state.errorMessage != null -> item(key = "error") {
                    CenteredNotice { CommentsError(message = state.errorMessage, onRetryClick = onRetryClick) }
                }

                state.comments.isEmpty() -> item(key = "empty") {
                    CenteredNotice {
                        Text(
                            text = "아직 댓글이 없어요",
                            style = MoaMapTheme.typography.body2,
                            color = MoaMapTheme.colors.textWhite,
                        )
                    }
                }

                else -> items(state.comments, key = { comment -> comment.id }) { comment ->
                    PostComment(comment = comment, mine = comment.authorId == state.myId)
                }
            }
        }

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 14.dp, top = CloseButtonTop)
                .size(CloseButtonSize)
                .clip(CircleShape)
                .background(CloseButtonColor)
                .clickable(role = Role.Button, onClick = onCloseClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "닫기",
                tint = MoaMapPrimitiveColors.White,
                modifier = Modifier.size(32.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .consumeTaps()
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
    }
}

/**
 * 댓글 한 줄. 시안 컴포넌트 「프라이빗 로그 댓글」.
 *
 * 남의 댓글은 왼쪽에 작성자 사진 18·이름(흰 글자)을 위에 두고, 내 댓글은 오른쪽에 말풍선만 둔다.
 */
@Composable
private fun PostComment(comment: MapPostComment, mine: Boolean) {
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
        Text(
            text = comment.content,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier
                .widthIn(max = CommentBubbleMaxWidth)
                .consumeTaps()
                .clip(CommentBubbleShape)
                .background(MoaMapPrimitiveColors.White)
                .padding(12.dp),
        )
    }
}

/** 검은 막 위라 목록의 회색 안내 대신 흰 글자로 둔다. */
@Composable
private fun CommentsError(message: String, onRetryClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = message,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textWhite,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetryClick) {
            Text(
                text = "다시 시도",
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textWhite,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9FA, widthDp = 393, heightDp = 852)
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
