package com.moamap.app.feature.mapdetail.presentation.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** 탭바가 위에 겹쳐 있어 그만큼 내려서 시작한다. */
internal val TabBarClearance = 90.dp

/** 오른쪽 아래 새 게시물 버튼(48dp)과 그 여백(20dp)을 비켜 가는 목록 아래 여백. */
internal val BottomClearance = 88.dp

/** 로그 탭 게시물을 보는 방식. */
enum class MapPostViewMode(val label: String) {
    Card("카드 형식"),
    Calendar("달력 형식"),
}

/** 시안의 카드 간격. 가로·세로가 같다. */
private val CardGap = 12.dp

private val CardShape = RoundedCornerShape(16.dp)
private val PhotoShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(1000.dp)
private val ChipShape = RoundedCornerShape(18.dp)

/** 시안의 사진 높이. 카드 폭이 바뀌어도 높이는 그대로다. */
private val PhotoHeight = 146.5.dp

/** 목록에서는 긴 글을 자른다. 전체는 상세에서 본다. */
private const val CONTENT_MAX_LINES = 6

/** 끝에서 이만큼 남았을 때 다음 페이지를 부른다. 끝에 완전히 닿기 전에 받아 두면 멈칫하지 않는다. */
private const val LOAD_MORE_THRESHOLD = 4

/**
 * 로그 탭 내용. 지도 멤버들이 남긴 게시물을 두 줄로 엇갈려 쌓는다.
 *
 * 카드 높이가 사진 유무·글 길이에 따라 달라 시안도 엇갈린 격자다. 줄을 맞추면 짧은 카드 옆에
 * 빈칸이 생긴다.
 */
@Composable
internal fun MapPostsContent(
    state: MapPostListUiState,
    onViewModeSelect: (MapPostViewMode) -> Unit,
    onSortSelect: (MapPostSort) -> Unit,
    onRetryClick: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
) {
    LoadMoreEffect(gridState = gridState, onLoadMore = onLoadMore)

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MoaMapDimens.ScreenHorizontalPadding,
            end = MoaMapDimens.ScreenHorizontalPadding,
            top = TabBarClearance,
            // 새 게시물 버튼이 떠 있어도 마지막 카드가 가리지 않게 버튼 높이만큼 더 띄운다.
            bottom = BottomClearance,
        ),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
        verticalItemSpacing = CardGap,
    ) {
        item(key = "toolbar", span = StaggeredGridItemSpan.FullLine) {
            PostsToolbar(
                viewMode = MapPostViewMode.Card,
                onViewModeSelect = onViewModeSelect,
                sort = state.sort,
                onSortSelect = onSortSelect,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        when {
            state.loading -> item(key = "loading", span = StaggeredGridItemSpan.FullLine) {
                CenteredNotice { Progress() }
            }

            state.errorMessage != null -> item(key = "error", span = StaggeredGridItemSpan.FullLine) {
                CenteredNotice {
                    ErrorNotice(message = state.errorMessage, onRetryClick = onRetryClick)
                }
            }

            state.posts.isEmpty() -> item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                CenteredNotice {
                    Text(
                        text = "아직 게시물이 없어요",
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapTheme.colors.textAssistive,
                    )
                }
            }

            else -> {
                items(state.posts, key = { post -> post.id }) { post ->
                    MapPostCard(post = post)
                }

                if (state.loadingMore) {
                    item(key = "loading-more", span = StaggeredGridItemSpan.FullLine) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Progress()
                        }
                    }
                } else if (state.loadMoreFailed) {
                    item(key = "load-more-failed", span = StaggeredGridItemSpan.FullLine) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            ErrorNotice(message = POST_LOAD_FAILED_MESSAGE, onRetryClick = onLoadMore)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 끝에 가까워지면 다음 페이지를 부른다.
 *
 * 항목 수도 함께 본다. "끝에 가까운가" 만 보면 첫 페이지 카드가 한 화면에 다 들어올 때 값이
 * 계속 참이라 변화가 없고, 다음 페이지를 끝내 부르지 않는다. 받는 중·끝 도달 같은 판단은
 * ViewModel 이 한다 - 여기서는 끝에 닿았다는 사실만 알린다.
 */
@Composable
private fun LoadMoreEffect(gridState: LazyStaggeredGridState, onLoadMore: () -> Unit) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(gridState) {
        snapshotFlow {
            val layout = gridState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            val nearEnd = lastVisible >= 0 && lastVisible >= layout.totalItemsCount - LOAD_MORE_THRESHOLD
            nearEnd to layout.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (nearEnd, _) -> nearEnd }
            .collect { currentOnLoadMore() }
    }
}

/**
 * 보기 방식 칩과 정렬.
 *
 * 칩을 누르면 아래로 다른 보기 방식이 펼쳐진다. 달력 형식에는 정렬이 없어 [sort] 를 null 로 넘기면 숨긴다.
 */
@Composable
internal fun PostsToolbar(
    viewMode: MapPostViewMode,
    onViewModeSelect: (MapPostViewMode) -> Unit,
    modifier: Modifier = Modifier,
    sort: MapPostSort? = null,
    onSortSelect: (MapPostSort) -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        ViewModeChip(viewMode = viewMode, onViewModeSelect = onViewModeSelect)

        if (sort != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortOption(
                    label = "등록순",
                    selected = sort == MapPostSort.Oldest,
                    onClick = { onSortSelect(MapPostSort.Oldest) },
                )
                SortOption(
                    label = "최신순",
                    selected = sort == MapPostSort.Latest,
                    onClick = { onSortSelect(MapPostSort.Latest) },
                )
            }
        }
    }
}

@Composable
private fun ViewModeChip(
    viewMode: MapPostViewMode,
    onViewModeSelect: (MapPostViewMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .clip(ChipShape)
            .background(MoaMapTheme.colors.textWhite)
            .border(1.dp, MoaMapPrimitiveColors.Gray300, ChipShape),
    ) {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = viewMode.label,
                style = MoaMapTheme.typography.button3,
                color = MoaMapTheme.colors.textNormal,
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = if (expanded) "보기 방식 닫기" else "보기 방식 펼치기",
                tint = MoaMapTheme.colors.textNormal,
                // 시안은 오른쪽 화살표를 돌려 쓴다. 닫혀 있으면 아래, 펼치면 위를 가리킨다.
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (expanded) -90f else 90f),
            )
        }

        if (expanded) {
            MapPostViewMode.entries
                .filterNot { mode -> mode == viewMode }
                .forEach { mode ->
                    Text(
                        text = mode.label,
                        style = MoaMapTheme.typography.button3,
                        color = MoaMapTheme.colors.textNormal,
                        modifier = Modifier
                            .clickable {
                                expanded = false
                                onViewModeSelect(mode)
                            }
                            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    )
                }
        }
    }
}

@Composable
private fun SortOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = if (selected) MoaMapTheme.typography.button2 else MoaMapTheme.typography.button3,
        color = if (selected) MoaMapTheme.colors.textNormal else MoaMapTheme.colors.textAssistive,
        // 글자 높이만으로는 누를 자리가 얇다. 세로로 넓힌다.
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}

/**
 * 게시물 카드.
 *
 * 사진이 있으면 첫 사진 위에 장소 이름을 얹고, 없으면 글 위 오른쪽에 장소 이름을 둔다.
 * 장소 태그가 여러 개여도 첫 번째만 보여준다. 카드 메뉴(수정·삭제)는 아직 없어 그리지 않는다.
 */
@Composable
internal fun MapPostCard(post: MapPost, modifier: Modifier = Modifier) {
    val photoUrl = post.imageUrls.firstOrNull()
    val placeName = post.placeNames.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MoaMapTheme.colors.textWhite)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (photoUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PhotoHeight)
                    .clip(PhotoShape)
                    .background(MoaMapTheme.colors.lineAlternative)
                    .border(1.dp, MoaMapTheme.colors.textWhite, PhotoShape),
            ) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (placeName != null) {
                    PlacePill(
                        name = placeName,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    )
                }
            }
        } else if (placeName != null) {
            PlacePill(name = placeName, modifier = Modifier.align(Alignment.End))
        }

        if (post.content.isNotBlank()) {
            Text(
                text = post.content,
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textNormal,
                maxLines = CONTENT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PlacePill(
    name: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MoaMapTheme.typography.caption0,
) {
    Text(
        text = name,
        style = textStyle,
        color = MoaMapTheme.colors.textNormal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(PillShape)
            .background(MoaMapPrimitiveColors.White.copy(alpha = 0.6f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
internal fun Progress() {
    CircularProgressIndicator(
        color = MoaMapTheme.colors.textAssistive,
        strokeWidth = 2.dp,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
internal fun ErrorNotice(message: String, onRetryClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = message,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetryClick) {
            Text(
                text = "다시 시도",
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textNormal,
            )
        }
    }
}

/** 목록 자리를 대신 채우는 안내. 세 상태가 같은 높이를 써야 오갈 때 덜컹이지 않는다. */
@Composable
internal fun CenteredNotice(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

internal val SampleMapPosts = listOf(
    MapPost(
        id = 1,
        authorId = 2,
        content = "성수 카페 다녀왔어요. 창가 자리가 좋아요",
        imageUrls = listOf("https://example.com/1.jpg"),
        placeNames = listOf("블루보틀 성수점"),
        createdAtMillis = null,
    ),
    MapPost(
        id = 2,
        authorId = 3,
        content = "주말엔 줄이 길어서 평일 오전을 추천해요. 디저트는 금방 떨어지니 일찍 가세요.",
        imageUrls = emptyList(),
        placeNames = listOf("연남 책방"),
        createdAtMillis = null,
    ),
    MapPost(
        id = 3,
        authorId = 2,
        content = "여기 야경 진짜 좋아요",
        imageUrls = listOf("https://example.com/3.jpg"),
        placeNames = emptyList(),
        createdAtMillis = null,
    ),
    MapPost(
        id = 4,
        authorId = 4,
        content = "장소 태그 없이 남긴 글",
        imageUrls = emptyList(),
        placeNames = emptyList(),
        createdAtMillis = null,
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFFF7F9FA, widthDp = 393, heightDp = 852)
@Composable
private fun MapPostsContentPreview() {
    MoaMapTheme {
        MapPostsContent(
            state = MapPostListUiState(loading = false, posts = SampleMapPosts, endReached = true),
            onViewModeSelect = {},
            onSortSelect = {},
            onRetryClick = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9FA, widthDp = 393, heightDp = 852)
@Composable
private fun MapPostsContentEmptyPreview() {
    MoaMapTheme {
        MapPostsContent(
            state = MapPostListUiState(loading = false),
            onViewModeSelect = {},
            onSortSelect = {},
            onRetryClick = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9FA, widthDp = 393, heightDp = 852)
@Composable
private fun MapPostsContentErrorPreview() {
    MoaMapTheme {
        MapPostsContent(
            state = MapPostListUiState(
                sort = MapPostSort.Oldest,
                loading = false,
                errorMessage = POST_LOAD_FAILED_MESSAGE,
            ),
            onViewModeSelect = {},
            onSortSelect = {},
            onRetryClick = {},
            onLoadMore = {},
        )
    }
}
