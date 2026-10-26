package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.MoaMapToast
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import com.moamap.app.feature.mapdetail.presentation.posts.MapPostListUiState
import com.moamap.app.feature.mapdetail.presentation.posts.CalendarDay
import com.moamap.app.feature.mapdetail.presentation.posts.MapPostCalendarContent
import com.moamap.app.feature.mapdetail.presentation.posts.MapPostCalendarUiState
import com.moamap.app.feature.mapdetail.presentation.posts.MapPostViewMode
import com.moamap.app.feature.mapdetail.presentation.posts.MapPostsContent
import com.moamap.app.feature.mapdetail.presentation.posts.SampleMapPosts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.MapDetailAction
import kotlinx.coroutines.launch

/** 시트가 가리지 않도록 지도 컨트롤을 시트 위로 띄우는 여백. */
private val MapControlsBottomGap = 16.dp

/** 완료 토스트 아래 끝과 장소 목록 시트 위 끝 사이. 시안 「내부 (방장뷰)」: 토스트 620~656, 시트 665. */
private val ToastSheetGap = 9.dp

/**
 * 접힌 시트 높이의 첫 값. 제목·검색창·카테고리 칩까지 보이는 높이다.
 *
 * 실제 높이는 시트가 머리 부분을 재서 알려 준다 - 글자 크기를 키운 기기에서 칩이 잘리지
 * 않게 하려는 것이고, 재기 전 첫 프레임에만 이 값을 쓴다.
 */
internal val DefaultSheetPeekHeight = 237.dp

@Composable
internal fun MapDetailContent(
    mapTitle: String,
    roleBadge: String?,
    action: MapDetailAction,
    actionEnabled: Boolean,
    placeCount: Int?,
    is3d: Boolean,
    canAddPlace: Boolean,
    myLocationInProgress: Boolean,
    selectedTab: MapDetailTab,
    onBackClick: () -> Unit,
    onActionClick: () -> Unit,
    showMenu: Boolean,
    onMenuClick: () -> Unit,
    on3dToggleClick: () -> Unit,
    onAddPlaceClick: () -> Unit,
    onMyLocationClick: () -> Unit,
    onTabSelected: (MapDetailTab) -> Unit,
    places: List<PlaceUiModel>,
    /** 값이 바뀌면 장소 목록 시트를 접는다. */
    collapseSheetSignal: Int,
    /** 장소 시트가 떠 있다. 그동안 목록 시트를 아래로 내려 숨기고, 닫히면 새로 올린다. */
    placeSheetOpen: Boolean = false,
    categoryFilters: List<PlaceCategoryFilter>,
    selectedCategory: PlaceCategoryFilter,
    onCategorySelect: (PlaceCategoryFilter) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPlaceClick: (Long) -> Unit,
    onLikeClick: (Long) -> Unit = {},
    postList: MapPostListUiState,
    onPostSortSelect: (MapPostSort) -> Unit,
    onPostsRetry: () -> Unit,
    onPostsLoadMore: () -> Unit,
    postViewMode: MapPostViewMode,
    onPostViewModeSelect: (MapPostViewMode) -> Unit,
    postCalendar: MapPostCalendarUiState,
    onCalendarPreviousMonth: () -> Unit,
    onCalendarNextMonth: () -> Unit,
    onCalendarDayClick: (CalendarDay) -> Unit,
    onCalendarRetry: () -> Unit,
    canWritePost: Boolean,
    /** 로그 탭 카드(카드 형식·달력 형식)를 누르면 게시물 상세를 연다. */
    onPostClick: (MapPost) -> Unit = {},
    onWritePostClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 공식지도. 시안대로 장소/로그 탭을 띄우지 않고, 장소 시트도 검색·칩·하트 없이 그린다. */
    official: Boolean = false,
    /**
     * 한 번 띄우는 완료 토스트. 장소 탭은 목록 시트 바로 위(펼친 목록이면 그 위 화면 아래), 로그 탭은 화면 아래에
     * 뜬다.
     */
    toastMessage: String? = null,
    onToastShown: () -> Unit = {},
    mapContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        MapDetailTopBar(
            mapTitle = mapTitle,
            roleBadge = roleBadge,
            action = action,
            actionEnabled = actionEnabled,
            onBackClick = onBackClick,
            onActionClick = onActionClick,
            showMenu = showMenu,
            onMenuClick = onMenuClick,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) {
            when (selectedTab) {
                MapDetailTab.Places -> {
                    MapDetailPlacesContent(
                        places = places,
                        placeCount = placeCount,
                        collapseSheetSignal = collapseSheetSignal,
                        placeSheetOpen = placeSheetOpen,
                        categoryFilters = categoryFilters,
                        selectedCategory = selectedCategory,
                        onCategorySelect = onCategorySelect,
                        searchQuery = searchQuery,
                        is3d = is3d,
                        canAddPlace = canAddPlace,
                        myLocationInProgress = myLocationInProgress,
                        onSearchQueryChange = onSearchQueryChange,
                        onPlaceClick = onPlaceClick,
                        onLikeClick = onLikeClick,
                        onTabSelected = onTabSelected,
                        on3dToggleClick = on3dToggleClick,
                        onAddPlaceClick = onAddPlaceClick,
                        onMyLocationClick = onMyLocationClick,
                        official = official,
                        toastMessage = toastMessage,
                        onToastShown = onToastShown,
                        mapContent = mapContent,
                    )
                }
                MapDetailTab.Logs -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (postViewMode) {
                            MapPostViewMode.Card -> MapPostsContent(
                                state = postList,
                                onViewModeSelect = onPostViewModeSelect,
                                onSortSelect = onPostSortSelect,
                                onRetryClick = onPostsRetry,
                                onLoadMore = onPostsLoadMore,
                                onPostClick = onPostClick,
                            )

                            MapPostViewMode.Calendar -> MapPostCalendarContent(
                                state = postCalendar,
                                onViewModeSelect = onPostViewModeSelect,
                                onPreviousMonthClick = onCalendarPreviousMonth,
                                onNextMonthClick = onCalendarNextMonth,
                                onDayClick = onCalendarDayClick,
                                onRetryClick = onCalendarRetry,
                                onPostClick = onPostClick,
                            )
                        }
                        MapDetailTabBar(
                            selectedTab = selectedTab,
                            onTabSelected = onTabSelected,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                        if (canWritePost) {
                            WritePostFab(
                                onClick = onWritePostClick,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 20.dp, bottom = 20.dp),
                            )
                        }
                        MoaMapToast(
                            message = toastMessage,
                            onShown = onToastShown,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 새 게시물 진입. 로그 탭에만 있다 - 장소 탭은 같은 자리를 3D·장소 추가가 쓴다. */
@Composable
private fun WritePostFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MoaMapPrimitiveColors.Blue500)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = "새 게시물",
            tint = MoaMapTheme.colors.textWhite,
            modifier = Modifier.size(32.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapDetailPlacesContent(
    places: List<PlaceUiModel>,
    placeCount: Int?,
    collapseSheetSignal: Int,
    placeSheetOpen: Boolean,
    categoryFilters: List<PlaceCategoryFilter>,
    selectedCategory: PlaceCategoryFilter,
    onCategorySelect: (PlaceCategoryFilter) -> Unit,
    searchQuery: String,
    is3d: Boolean,
    canAddPlace: Boolean,
    myLocationInProgress: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onPlaceClick: (Long) -> Unit,
    onLikeClick: (Long) -> Unit,
    onTabSelected: (MapDetailTab) -> Unit,
    on3dToggleClick: () -> Unit,
    onAddPlaceClick: () -> Unit,
    onMyLocationClick: () -> Unit,
    official: Boolean,
    toastMessage: String?,
    onToastShown: () -> Unit,
    mapContent: @Composable () -> Unit,
) {
    // 장소 시트가 떠 있는 동안에만 목록 시트를 숨길 수 있다. 사람이 끌어서 숨기지는 못한다.
    val currentPlaceSheetOpen by rememberUpdatedState(placeSheetOpen)
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false,
            confirmValueChange = { value -> value != SheetValue.Hidden || currentPlaceSheetOpen },
        ),
    )
    val scope = rememberCoroutineScope()

    // 접혔을 때 카테고리 칩까지 보이는 높이. 시트가 머리 부분을 재서 알려 준다.
    var peekHeight by remember { mutableStateOf(DefaultSheetPeekHeight) }

    // 장소 시트가 닫히면 목록 시트를 이 높이로 다시 올린다. 장소 시트가 없으면 null.
    var restoreAfterPlaceSheet by rememberSaveable { mutableStateOf<SheetValue?>(null) }

    // 장소 상세를 닫기(X)로 나오면 처음 들어왔을 때처럼 접힌 높이로 올린다.
    // 아래 효과보다 먼저 둔다 - 같은 순간에 장소 시트가 닫혀도 펼친 높이로 올리지 않게 기억부터 지운다.
    LaunchedEffect(collapseSheetSignal) {
        if (collapseSheetSignal > 0) {
            restoreAfterPlaceSheet = null
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    // 장소 시트가 떠 있는 동안 목록 시트를 아래로 완전히 내려 둔다. 접어 두기만 하면 장소 시트가
    // 내려갈 때 접힌 목록이 비쳐 시트 위에 시트가 쌓여 보인다(10-06 사용자 지적). 장소 시트가 닫히면
    // 아래에서 새로 올린다 - 펼쳐 두었으면 펼친 높이로, 아니면 접힌 높이로. 화면을 돌려 다시 들어와도
    // 기억은 남는다.
    LaunchedEffect(placeSheetOpen) {
        val sheetState = scaffoldState.bottomSheetState
        if (placeSheetOpen) {
            if (restoreAfterPlaceSheet == null) {
                restoreAfterPlaceSheet = if (sheetState.currentValue == SheetValue.Expanded) {
                    SheetValue.Expanded
                } else {
                    SheetValue.PartiallyExpanded
                }
            }
            sheetState.hide()
        } else {
            val restore = restoreAfterPlaceSheet
            restoreAfterPlaceSheet = null
            when (restore) {
                SheetValue.Expanded -> sheetState.expand()
                SheetValue.PartiallyExpanded -> sheetState.partialExpand()
                else -> Unit
            }
        }
    }

    // 토스트는 시트보다 위 층에 둔다. 지도 층에 두면 목록을 펼쳐 둔 채 장소를 지웠을 때 다시 펼쳐진 시트가 덮는다.
    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
                MapDetailBottomSheet(
                    places = places,
                    placeCount = placeCount,
                    categoryFilters = categoryFilters,
                    selectedCategory = selectedCategory,
                    onCategorySelect = onCategorySelect,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    // 접힌 시트에서 검색창을 누르면 목록이 안 보인다. 눌린 김에 끝까지 올린다.
                    onSearchFocused = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                    onPlaceClick = onPlaceClick,
                    onLikeClick = onLikeClick,
                    onHeaderHeightChange = { height -> peekHeight = height },
                    official = official,
                )
            },
            sheetPeekHeight = peekHeight,
            sheetShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp),
            sheetContainerColor = MoaMapTheme.colors.backgroundSecondary,
            sheetTonalElevation = 0.dp,
            sheetShadowElevation = 10.dp,
            sheetDragHandle = null,
            modifier = Modifier.fillMaxSize(),
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                mapContent()
                // 장소 시트가 떠 있는 동안 지도 위 탭·버튼을 숨긴다. 시트 위가 비친 지도라 버튼이 시트 가장자리에
                // 걸쳐 보이고, 로그 탭을 누르면 시트 뒤 화면만 바뀐다.
                if (!placeSheetOpen) {
                    if (!official) {
                        MapDetailTabBar(
                            selectedTab = MapDetailTab.Places,
                            onTabSelected = onTabSelected,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                    MyLocationButton(
                        inProgress = myLocationInProgress,
                        onClick = onMyLocationClick,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, bottom = peekHeight + MapControlsBottomGap),
                    )
                    MapDetailMapControls(
                        is3d = is3d,
                        canAddPlace = canAddPlace,
                        on3dToggleClick = on3dToggleClick,
                        onAddPlaceClick = onAddPlaceClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = peekHeight + MapControlsBottomGap),
                    )
                }
            }
        }
        // 접힌 시트면 그 위 [ToastSheetGap], 펼친 시트면 목록 위 화면 아래 20(로그 탭과 같은 자리)이다.
        val sheetExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
        MoaMapToast(
            message = toastMessage,
            onShown = onToastShown,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (sheetExpanded) 20.dp else peekHeight + ToastSheetGap),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapDetailScreenPreview() {
    MoaMapTheme {
        MapDetailContent(
            mapTitle = "서울 데이트 지도",
            roleBadge = "방장",
            action = MapDetailAction.Leave,
            actionEnabled = true,
            placeCount = 32,
            is3d = true,
            canAddPlace = true,
            myLocationInProgress = false,
            selectedTab = MapDetailTab.Places,
            onBackClick = {},
            onActionClick = {},
            showMenu = true,
            onMenuClick = {},
            on3dToggleClick = {},
            onAddPlaceClick = {},
            onMyLocationClick = {},
            onTabSelected = {},
            places = SamplePlaces,
            collapseSheetSignal = 0,
            categoryFilters = categoryFilters(SamplePlaces),
            selectedCategory = PlaceCategoryFilter.All,
            onCategorySelect = {},
            searchQuery = "",
            onSearchQueryChange = {},
            onPlaceClick = {},
            postList = MapPostListUiState(loading = false, posts = SampleMapPosts, endReached = true),
            onPostSortSelect = {},
            onPostsRetry = {},
            onPostsLoadMore = {},
            postViewMode = MapPostViewMode.Card,
            onPostViewModeSelect = {},
            postCalendar = MapPostCalendarUiState(),
            onCalendarPreviousMonth = {},
            onCalendarNextMonth = {},
            onCalendarDayClick = {},
            onCalendarRetry = {},
            canWritePost = true,
            onWritePostClick = {},
            mapContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MoaMapPrimitiveColors.Blue50),
                )
            },
        )
    }
}

/** 미리보기로 들어온 상태. 참여하기가 뜨고 장소 추가는 잠겨 있다. */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapDetailScreenNotJoinedPreview() {
    MoaMapTheme {
        MapDetailContent(
            mapTitle = "성수 카페 투어",
            roleBadge = null,
            action = MapDetailAction.Join,
            actionEnabled = true,
            placeCount = 12,
            is3d = false,
            canAddPlace = false,
            myLocationInProgress = false,
            selectedTab = MapDetailTab.Places,
            onBackClick = {},
            onActionClick = {},
            showMenu = false,
            onMenuClick = {},
            on3dToggleClick = {},
            onAddPlaceClick = {},
            onMyLocationClick = {},
            onTabSelected = {},
            places = SamplePlaces,
            collapseSheetSignal = 0,
            categoryFilters = categoryFilters(SamplePlaces),
            selectedCategory = PlaceCategoryFilter.All,
            onCategorySelect = {},
            searchQuery = "",
            onSearchQueryChange = {},
            onPlaceClick = {},
            postList = MapPostListUiState(loading = false, posts = SampleMapPosts, endReached = true),
            onPostSortSelect = {},
            onPostsRetry = {},
            onPostsLoadMore = {},
            postViewMode = MapPostViewMode.Card,
            onPostViewModeSelect = {},
            postCalendar = MapPostCalendarUiState(),
            onCalendarPreviousMonth = {},
            onCalendarNextMonth = {},
            onCalendarDayClick = {},
            onCalendarRetry = {},
            canWritePost = true,
            onWritePostClick = {},
            mapContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MoaMapPrimitiveColors.Blue50),
                )
            },
        )
    }
}
