package com.moamap.app.feature.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.designsystem.component.CardShadowColor
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapLoadingIndicator
import com.moamap.app.core.designsystem.component.MoaMapRetryText
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.notification.domain.model.NotificationKind
import com.moamap.app.feature.notification.domain.model.UserNotification
import com.moamap.app.feature.notification.presentation.NotificationDestination
import com.moamap.app.feature.notification.presentation.NotificationListUiState
import com.moamap.app.feature.notification.presentation.NotificationListViewModel
import com.moamap.app.feature.notification.presentation.formatNotificationTime
import com.moamap.app.feature.notification.presentation.iconRes
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** 끝에서 이만큼 남으면 다음 페이지를 부른다. 공지사항과 같다. */
private const val LOAD_MORE_THRESHOLD = 4

private val NotificationCardShape = RoundedCornerShape(12.dp)
private val UnreadDotSize = 8.dp

/**
 * 알림. 홈 종 아이콘으로 들어온다. 시안 릴리즈 v1 「알림」(`4925:20123`).
 *
 * @param onDestination 알림을 눌러 갈 곳이 있을 때. 갈 곳이 없는 알림은 읽음으로만 바뀐다.
 */
@Composable
internal fun NotificationScreen(
    onBackClick: () -> Unit,
    onDestination: (NotificationDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onNotificationClick = { notification -> viewModel.open(notification)?.let(onDestination) },
        onMarkAllReadClick = viewModel::markAllRead,
        onRetryClick = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        modifier = modifier,
    )
}

@Composable
private fun NotificationContent(
    uiState: NotificationListUiState,
    onBackClick: () -> Unit,
    onNotificationClick: (UserNotification) -> Unit,
    onMarkAllReadClick: () -> Unit,
    onRetryClick: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            val nearEnd = lastVisible >= 0 && lastVisible >= layout.totalItemsCount - LOAD_MORE_THRESHOLD
            // 항목 수도 같이 본다. 첫 페이지가 한 화면에 다 들어오면 스크롤 없이도 다음 페이지가 필요하다.
            nearEnd to layout.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (nearEnd, _) -> nearEnd }
            .collect { currentOnLoadMore() }
    }

    val empty = !uiState.loading && uiState.errorMessage == null && uiState.notifications.isEmpty()
    // 지금 시각은 화면을 그릴 때 한 번 잡는다. 목록을 보는 사이 「방금」이 바뀌지 않아도 된다.
    val nowMillis = remember(uiState.notifications) { System.currentTimeMillis() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            MoaMapTitleTopBar(title = "알림", onBackClick = onBackClick)
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.loading -> MoaMapLoadingIndicator(modifier = Modifier.align(Alignment.Center))

                    uiState.errorMessage != null -> MoaMapErrorNotice(
                        message = uiState.errorMessage,
                        onRetryClick = onRetryClick,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    empty -> Unit

                    // 시안: 상단 바 아래 12 에 「모두 읽음」, 그 아래 12 에 카드(사이 8).
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = MoaMapDimens.ScreenHorizontalPadding,
                            end = MoaMapDimens.ScreenHorizontalPadding,
                            top = 12.dp,
                            bottom = 20.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "markAllRead") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(
                                    text = "모두 읽음",
                                    style = MoaMapTheme.typography.button2,
                                    color = MoaMapTheme.colors.textAlternative,
                                    modifier = Modifier.clickable(role = Role.Button, onClick = onMarkAllReadClick),
                                )
                            }
                        }
                        items(uiState.notifications, key = { notification -> notification.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                nowMillis = nowMillis,
                                onClick = { onNotificationClick(notification) },
                            )
                        }
                        if (uiState.loadingMore) {
                            item(key = "loadingMore") {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    MoaMapLoadingIndicator()
                                }
                            }
                        }
                        if (uiState.loadMoreFailed) {
                            item(key = "loadMoreFailed") {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    MoaMapRetryText(onClick = onLoadMore)
                                }
                            }
                        }
                    }
                }
            }
        }
        // 공지사항 빈 화면처럼 화면 전체의 가운데에 둔다(10-26 사용자 결정, 시안 없음).
        if (empty) {
            Text(
                text = "아직 받은 알림이 없어요",
                style = MoaMapTheme.typography.subtitle2.withDesignLineHeight(),
                color = MoaMapTheme.colors.textAlternative,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/**
 * 알림 카드. 시안 「Alarm」: 안쪽 16, 모서리 12, 아이콘 24 · 문구 subtitle4 위에 시각 caption0(사이 4).
 * 안 읽은 알림은 흰 바탕에 그림자와 오른쪽 파란 점 8, 읽은 알림은 회색 바탕에 테두리 1 이다.
 */
@Composable
private fun NotificationCard(
    notification: UserNotification,
    nowMillis: Long,
    onClick: () -> Unit,
) {
    val unread = !notification.read
    val timeLabel = notification.createdAtMillis?.let { createdAt -> formatNotificationTime(createdAt, nowMillis) }

    ShadowedSurface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (unread) contentDescription = "안 읽음" },
        shape = NotificationCardShape,
        color = if (unread) MoaMapPrimitiveColors.White else MoaMapTheme.colors.backgroundSecondary,
        // 시안: 안 읽은 카드만 그림자 `0 0 8 4%`(공용 카드 그림자와 같다).
        shadowColor = if (unread) CardShadowColor else Color.Transparent,
        border = if (unread) null else BorderStroke(1.dp, MoaMapTheme.colors.lineAlternative),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(notification.kind.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = notification.message,
                    style = MoaMapTheme.typography.subtitle4.withDesignLineHeight(),
                    color = MoaMapTheme.colors.textNormal,
                )
                if (timeLabel != null) {
                    Text(
                        text = timeLabel,
                        style = MoaMapTheme.typography.caption0.withDesignLineHeight(),
                        color = MoaMapTheme.colors.textAssistive,
                    )
                }
            }
            if (unread) {
                Box(
                    modifier = Modifier
                        .size(UnreadDotSize)
                        .background(MoaMapPrimitiveColors.Blue600, CircleShape),
                )
            }
        }
    }
}

/** 프리뷰 전용 표본. 지어낸 알림이라 화면에 넘기지 않는다. */
private val PreviewNotifications = listOf(
    UserNotification(1L, NotificationKind.MapAdminAssigned, "모아님이 나를 지도 관리자로 지정했어요.", 10L, 10L, false, 0L),
    UserNotification(2L, NotificationKind.MapPostCommented, "커비님이 내 게시글에 댓글을 남겼어요.", 3L, 10L, false, 0L),
    UserNotification(3L, NotificationKind.PlaceApproved, "등록 요청한 장소가 승인됐어요.", 5L, 10L, true, 0L),
    UserNotification(4L, NotificationKind.PlaceRejected, "등록 요청한 장소가 반려됐어요.", 6L, 10L, true, 0L),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun NotificationScreenPreview() {
    MoaMapTheme {
        NotificationContent(
            uiState = NotificationListUiState(loading = false, notifications = PreviewNotifications),
            onBackClick = {},
            onNotificationClick = {},
            onMarkAllReadClick = {},
            onRetryClick = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun NotificationScreenEmptyPreview() {
    MoaMapTheme {
        NotificationContent(
            uiState = NotificationListUiState(loading = false),
            onBackClick = {},
            onNotificationClick = {},
            onMarkAllReadClick = {},
            onRetryClick = {},
            onLoadMore = {},
        )
    }
}
