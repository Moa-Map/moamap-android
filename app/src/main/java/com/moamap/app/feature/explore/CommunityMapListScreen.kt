package com.moamap.app.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ListCardShadowColor
import com.moamap.app.core.designsystem.component.MoaMapBackButton
import com.moamap.app.core.designsystem.component.MoaMapSearchBar
import com.moamap.app.core.designsystem.component.MoaMapTopBarIconEdgePadding
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.presentation.CommunityMapCard
import com.moamap.app.feature.explore.presentation.CommunityMapListUiState
import com.moamap.app.feature.explore.presentation.CommunityMapListViewModel
import com.moamap.app.feature.explore.presentation.CommunityMapSortRow
import com.moamap.app.feature.explore.presentation.CommunityMapsError
import com.moamap.app.feature.explore.presentation.CommunityMapsPlaceholder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** 끝에서 이만큼 남으면 다음 페이지를 부른다. 로그 탭 게시물 목록과 같다. */
private const val LOAD_MORE_THRESHOLD = 4

private const val ALL_TAG_LABEL = "전체"

private val TopBarHeight = 58.dp
private val ChipShape = RoundedCornerShape(1000.dp)

/**
 * 커뮤니티 지도 전체보기. 시안 「Home/커뮤니티지도」(`3258:14163`).
 *
 * 검색창에 글자가 있으면 칩·정렬을 숨기고 같은 목록 자리에 검색 결과를 그린다. 결과가 없으면 아무것도
 * 띄우지 않는다. 지우면 검색 전 칩·정렬의 목록으로 돌아간다(10-10 사용자 결정).
 */
@Composable
fun CommunityMapListScreen(
    onBackClick: () -> Unit,
    onMapClick: (CommunityMap) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CommunityMapListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 처음이면 첫 페이지를, 지도에 들어갔다 돌아왔으면 받아 둔 만큼을 다시 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    CommunityMapListContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onMapClick = onMapClick,
        onTagClick = viewModel::selectTag,
        onSortClick = viewModel::selectSort,
        onRetryClick = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onQueryChange = viewModel::updateQuery,
        onSearch = viewModel::searchNow,
        modifier = modifier,
    )
}

@Composable
private fun CommunityMapListContent(
    uiState: CommunityMapListUiState,
    onBackClick: () -> Unit,
    onMapClick: (CommunityMap) -> Unit,
    onTagClick: (String?) -> Unit,
    onSortClick: (CommunityMapSort) -> Unit,
    onRetryClick: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit = {},
    onSearch: () -> Unit = {},
) {
    val searching = uiState.searchKeyword != null
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundPrimary)
            .statusBarsPadding(),
    ) {
        CommunityMapListTopBar(onBackClick = onBackClick)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                top = 20.dp,
                bottom = 20.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            item(key = "search") {
                SearchBar(
                    query = uiState.query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    modifier = Modifier.padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
                )
            }
            if (searching) {
                // 칩·정렬 묶음이 빠진 자리. 시안의 검색창 묶음과 목록 묶음 사이 20 을 그대로 둔다.
                item(key = "search-gap") { Spacer(Modifier.height(20.dp)) }
            } else {
                item(key = "chips") {
                    Spacer(Modifier.height(12.dp))
                    TagChipRow(tags = uiState.tags, selected = uiState.selectedTag, onClick = onTagClick)
                }
                item(key = "sort") {
                    Spacer(Modifier.height(20.dp))
                    CommunityMapSortRow(
                        selected = uiState.sort,
                        onClick = onSortClick,
                        modifier = Modifier.padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            when {
                uiState.loading -> item(key = "loading") {
                    CommunityMapsPlaceholder { CircularProgressIndicator() }
                }

                uiState.errorMessage != null -> {
                    val message = uiState.errorMessage
                    item(key = "error") {
                        CommunityMapsPlaceholder {
                            CommunityMapsError(message = message, onRetryClick = onRetryClick)
                        }
                    }
                }

                // 검색 결과가 없으면 아무것도 띄우지 않는다.
                uiState.maps.isEmpty() && searching -> Unit

                uiState.maps.isEmpty() -> item(key = "empty") {
                    CommunityMapsPlaceholder {
                        Text(
                            text = if (uiState.selectedTag == null) {
                                "아직 등록된 지도가 없어요"
                            } else {
                                "이 태그의 지도가 없어요"
                            },
                            style = MoaMapTheme.typography.body2,
                            color = MoaMapTheme.colors.textAssistive,
                        )
                    }
                }

                else -> items(uiState.maps, key = { map -> map.id }) { map ->
                    CommunityMapCard(
                        map = map,
                        onClick = { onMapClick(map) },
                        modifier = Modifier
                            .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
                            .padding(bottom = 8.dp),
                    )
                }
            }

            if (uiState.loadingMore) {
                item(key = "loadingMore") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
            if (uiState.loadMoreFailed) {
                item(key = "loadMoreFailed") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "다시 시도",
                            style = MoaMapTheme.typography.button2,
                            color = MoaMapTheme.colors.textNormal,
                            modifier = Modifier.clickable(role = Role.Button, onClick = onLoadMore),
                        )
                    }
                }
            }
        }
    }
}

/** 시안 GNB: 높이 58, 왼쪽 20 에 뒤로가기 32, 가운데 제목. */
@Composable
private fun CommunityMapListTopBar(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TopBarHeight),
    ) {
        // 누르는 자리는 48 로 넓히고 아이콘이 왼쪽 20 에 서도록 12 만 띄운다.
        MoaMapBackButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = MoaMapTopBarIconEdgePadding),
        )
        Text(
            text = "커뮤니티 지도",
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/**
 * 검색창. 입력을 멈추면 찾고([CommunityMapListViewModel.updateQuery]), 키보드 검색 버튼은 바로 찾는다.
 *
 * 서버는 지도만 찾지만 안내 문구는 시안 그대로 둔다(10-10 사용자 결정).
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    MoaMapSearchBar(modifier = modifier) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MoaMapTheme.typography.body2.copy(color = MoaMapTheme.colors.textNormal),
            cursorBrush = SolidColor(MoaMapPrimitiveColors.Blue500),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch()
                    keyboardController?.hide()
                },
            ),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = "장소,지도를 검색해보세요",
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapTheme.colors.textAssistive,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            },
        )
    }
}

/** 칩 줄. 「전체」 다음에 서버 지도에서 모은 태그가 온다. 좌우 끝까지 흘러가도록 여백을 콘텐츠 패딩으로 준다. */
@Composable
private fun TagChipRow(
    tags: List<String>,
    selected: String?,
    onClick: (String?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = MoaMapDimens.ScreenHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = ALL_TAG_LABEL) {
            TagChip(label = ALL_TAG_LABEL, selected = selected == null, onClick = { onClick(null) })
        }
        items(tags, key = { tag -> "tag:$tag" }) { tag ->
            TagChip(label = tag, selected = tag == selected, onClick = { onClick(tag) })
        }
    }
}

/** 시안 「Chip」 Theme=Mono: 여백 20/8, 14 Regular, 그림자 0 0 10 8%. 고르면 짙은 회색에 흰 글자. */
@Composable
private fun TagChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ShadowedSurface(
        shape = ChipShape,
        color = if (selected) MoaMapPrimitiveColors.Gray800 else MoaMapPrimitiveColors.White,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ListCardShadowColor,
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MoaMapTheme.typography.button3.withDesignLineHeight(),
            color = if (selected) MoaMapTheme.colors.textWhite else MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun CommunityMapListPreview() {
    MoaMapTheme {
        CommunityMapListContent(
            uiState = CommunityMapListUiState(
                tags = listOf("카페", "데이트", "산책", "맛집"),
                loading = false,
                maps = List(4) { index ->
                    CommunityMap(
                        id = index.toLong(),
                        title = "지도 이름 $index",
                        imageUrl = null,
                        hashtags = listOf("카페", "데이트"),
                        memberCount = 12,
                        placeCount = 30,
                        joined = false,
                    )
                },
            ),
            onBackClick = {},
            onMapClick = {},
            onTagClick = {},
            onSortClick = {},
            onRetryClick = {},
            onLoadMore = {},
        )
    }
}
