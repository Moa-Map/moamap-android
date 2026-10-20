package com.moamap.app.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.designsystem.component.ListCardShadowColor
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapLoadingIndicator
import com.moamap.app.core.designsystem.component.MoaMapRetryText
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.mypage.domain.model.Notice
import com.moamap.app.feature.mypage.presentation.NoticeDetailUiState
import com.moamap.app.feature.mypage.presentation.NoticeDetailViewModel
import com.moamap.app.feature.mypage.presentation.NoticeListUiState
import com.moamap.app.feature.mypage.presentation.NoticeListViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 끝에서 이만큼 남으면 다음 페이지를 부른다. 커뮤니티 지도 전체보기와 같다. */
private const val LOAD_MORE_THRESHOLD = 4

/** 목록 카드의 본문 미리보기 줄 수(10-20 사용자 결정). */
private const val PREVIEW_BODY_MAX_LINES = 2

private const val NOTICE_DATE_PATTERN = "yyyy.MM.dd"

private val NoticeCardShape = RoundedCornerShape(12.dp)

/** 시안 카드 그림자 `0 0 10 rgba(0,0,0,0.08)`. */
private val NoticeCardShadowBlurRadius = 10.dp

/** 등록일. 기기 시간대 기준 `2026.10.20`(10-20 사용자 결정). */
internal fun formatNoticeDate(millis: Long): String =
    SimpleDateFormat(NOTICE_DATE_PATTERN, Locale.KOREA).format(Date(millis))

/** 설정 「공지사항」. 시안 「공지사항」(`3738:8796`), 공지가 없으면 「공지사항 default」(`3881:20241`). */
@Composable
internal fun NoticeListScreen(
    onBackClick: () -> Unit,
    onNoticeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoticeListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NoticeListContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onNoticeClick = onNoticeClick,
        onRetryClick = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        modifier = modifier,
    )
}

@Composable
private fun NoticeListContent(
    uiState: NoticeListUiState,
    onBackClick: () -> Unit,
    onNoticeClick: (Long) -> Unit,
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

    val empty = !uiState.loading && uiState.errorMessage == null && uiState.notices.isEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        NoticeScaffold(onBackClick = onBackClick) {
            when {
                uiState.loading -> MoaMapLoadingIndicator(modifier = Modifier.align(Alignment.Center))

                uiState.errorMessage != null -> MoaMapErrorNotice(
                    message = uiState.errorMessage,
                    onRetryClick = onRetryClick,
                    modifier = Modifier.align(Alignment.Center),
                )

                empty -> Unit

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = MoaMapDimens.ScreenHorizontalPadding,
                        end = MoaMapDimens.ScreenHorizontalPadding,
                        top = 20.dp,
                        bottom = 20.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.notices, key = { notice -> notice.id }) { notice ->
                        NoticeCard(
                            notice = notice,
                            bodyMaxLines = PREVIEW_BODY_MAX_LINES,
                            onClick = { onNoticeClick(notice.id) },
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
        // 시안은 상단 바 아래 공간이 아니라 화면 전체의 가운데에 둔다.
        if (empty) NoticeEmpty(modifier = Modifier.align(Alignment.Center))
    }
}

/** 공지 상세. 시안이 없어 목록 카드에 본문을 다 펼쳐 그린다(10-20 사용자 결정). */
@Composable
internal fun NoticeDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoticeDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NoticeDetailContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun NoticeDetailContent(
    uiState: NoticeDetailUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NoticeScaffold(onBackClick = onBackClick, modifier = modifier) {
        val notice = uiState.notice
        when {
            uiState.loading -> MoaMapLoadingIndicator(modifier = Modifier.align(Alignment.Center))

            notice == null -> MoaMapErrorNotice(
                message = uiState.errorMessage.orEmpty(),
                onRetryClick = onRetryClick,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
                    .padding(vertical = 20.dp),
            ) {
                NoticeCard(notice = notice, bodyMaxLines = Int.MAX_VALUE, onClick = null)
            }
        }
    }
}

/** 목록·상세 공통 틀: 회색 바탕 + 상단 바 「공지사항」 + 그 아래 내용 자리. */
@Composable
private fun NoticeScaffold(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        MoaMapTitleTopBar(title = "공지사항", onBackClick = onBackClick)
        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}

/**
 * 공지 카드. 흰 바탕·안쪽 16·모서리 12, 제목 → 본문 → 날짜를 12 간격으로 쌓는다.
 *
 * 세로로 쌓는 글자라 줄 높이를 살린다. 시안 간격 12 는 줄 높이 상자 사이 거리다.
 *
 * @param onClick null 이면 누를 수 없다(상세).
 */
@Composable
private fun NoticeCard(
    notice: Notice,
    bodyMaxLines: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val dateLabel = remember(notice.createdAtMillis) { notice.createdAtMillis?.let(::formatNoticeDate) }

    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = NoticeCardShape,
        shadowBlurRadius = NoticeCardShadowBlurRadius,
        shadowColor = ListCardShadowColor,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = notice.title,
                style = MoaMapTheme.typography.subtitle2.withDesignLineHeight(),
                color = MoaMapTheme.colors.textNormal,
            )
            Text(
                text = notice.content,
                style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                color = MoaMapTheme.colors.textNormal,
                maxLines = bodyMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (dateLabel != null) {
                Text(
                    text = dateLabel,
                    style = MoaMapTheme.typography.caption0.withDesignLineHeight(),
                    color = MoaMapTheme.colors.textAssistive,
                )
            }
        }
    }
}

/** 공지가 하나도 없을 때. 시안 「공지사항 default」. */
@Composable
private fun NoticeEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "등록된 공지사항이 없어요",
            style = MoaMapTheme.typography.subtitle2.withDesignLineHeight(),
            color = MoaMapTheme.colors.textAlternative,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "새로운 소식이 생기면 이곳에서 알려드릴게요",
            style = MoaMapTheme.typography.body2.withDesignLineHeight(),
            color = MoaMapTheme.colors.textAssistive,
            textAlign = TextAlign.Center,
        )
    }
}

private val PreviewNotices = listOf(
    Notice(
        id = 1L,
        title = "모아맵 서비스 점검 안내",
        content = "10월 25일 새벽 2시부터 4시까지 서버 점검이 있어요. 점검 중에는 지도와 장소를 불러올 수 없어요. " +
            "이용에 불편을 드려 죄송합니다.",
        createdAtMillis = 1_792_454_400_000L,
    ),
    Notice(
        id = 2L,
        title = "공식 지도에 공중화장실 지도가 추가됐어요",
        content = "서울 공중화장실 위치를 지도에서 바로 찾아보세요.",
        createdAtMillis = 1_791_849_600_000L,
    ),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun NoticeListPreview() {
    MoaMapTheme {
        NoticeListContent(
            uiState = NoticeListUiState(loading = false, notices = PreviewNotices, endReached = true),
            onBackClick = {},
            onNoticeClick = {},
            onRetryClick = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun NoticeListEmptyPreview() {
    MoaMapTheme {
        NoticeListContent(
            uiState = NoticeListUiState(loading = false, endReached = true),
            onBackClick = {},
            onNoticeClick = {},
            onRetryClick = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun NoticeDetailPreview() {
    MoaMapTheme {
        NoticeDetailContent(
            uiState = NoticeDetailUiState(loading = false, notice = PreviewNotices.first()),
            onBackClick = {},
            onRetryClick = {},
        )
    }
}
