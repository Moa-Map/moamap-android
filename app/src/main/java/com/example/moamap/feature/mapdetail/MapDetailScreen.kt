package com.example.moamap.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moamap.core.designsystem.component.ErrorSnackbar
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.mapdetail.domain.model.MapDetailAction
import com.example.moamap.feature.mapdetail.presentation.MapDetailViewModel
import com.example.moamap.feature.mapdetail.presentation.addplace.AddPlaceSheet
import com.example.moamap.feature.mapdetail.presentation.addplace.AddPlaceViewModel
import com.example.moamap.feature.mapdetail.presentation.mapOrNull
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.animation.MapAnimationOptions

/** 시트가 가리지 않도록 지도 컨트롤을 시트 위로 띄우는 여백. */
private val MapControlsBottomGap = 16.dp

private val SheetPeekHeight = 283.dp

private val MapDetailUiStateSaver = listSaver<MapDetailUiState, String>(
    save = { state ->
        listOf(
            state.selectedTab.name,
            state.selectedCategory,
            state.selectedPlaceId?.toString().orEmpty(),
        )
    },
    restore = { values ->
        MapDetailUiState(
            selectedTab = values.getOrNull(0)?.let { savedTabName ->
                MapDetailTab.entries.firstOrNull { tab -> tab.name == savedTabName }
            } ?: MapDetailTab.Places,
            selectedCategory = values.getOrNull(1) ?: "전체",
            selectedPlaceId = values.getOrNull(2)?.toLongOrNull(),
        )
    },
)

@Composable
fun MapDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 서버 응답이 오기 전 상단바를 채우는 초기값. 응답이 도착하면 덮어쓴다. */
    initialTitle: String = "",
    viewModel: MapDetailViewModel = hiltViewModel(),
    addPlaceViewModel: AddPlaceViewModel = hiltViewModel(),
) {
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()

    // 나가기가 끝나면 왔던 곳(탐색 또는 모음)으로 돌아간다.
    LaunchedEffect(screenState.left) {
        if (screenState.left) onBackClick()
    }

    // 등록 완료 안내. 시트가 닫힌 뒤 상세 화면에서 띄운다.
    var addPlaceNotice by remember { mutableStateOf<String?>(null) }
    var addPlaceSheetVisible by rememberSaveable { mutableStateOf(false) }

    var uiState by rememberSaveable(stateSaver = MapDetailUiStateSaver) {
        mutableStateOf(MapDetailUiState())
    }
    val selectedPlace = SamplePlaces.firstOrNull { place ->
        place.id == uiState.selectedPlaceId
    }
    val closePlaceDetail = { uiState = uiState.closePlaceDetail() }
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(MapDetailCenter)
            zoom(MapDetailDefaultZoom)
        }
    }
    val onMarkerClick: (Long) -> Unit = remember {
        { placeId -> uiState = uiState.selectPlace(placeId) }
    }
    val onClusterClick: (MarkerCluster) -> Unit = remember(mapViewportState) {
        { cluster ->
            // 중심과 줌만 건드린다. 여기서 pitch 를 걸면 2D 로 보던 사람이 클러스터를
            // 누를 때마다 지도가 기울어진다.
            mapViewportState.easeTo(
                cameraOptions {
                    center(cluster.anchorPoint())
                    zoom((mapViewportState.cameraState?.zoom ?: MapDetailDefaultZoom) + 1.5)
                },
                MapAnimationOptions.mapAnimationOptions { duration(600L) },
            )
        }
    }
    var is3d by rememberSaveable { mutableStateOf(true) }
    val on3dToggleClick: () -> Unit = remember(mapViewportState) {
        {
            val next = !is3d
            is3d = next
            mapViewportState.easeTo(
                cameraOptions { pitch(if (next) MapDetailPitch else 0.0) },
                MapAnimationOptions.mapAnimationOptions { duration(400L) },
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapDetailContent(
            mapTitle = screenState.title ?: initialTitle,
            roleBadge = screenState.roleBadge,
            action = screenState.action,
            actionEnabled = !screenState.actionInProgress,
            placeCount = screenState.placeCount,
            is3d = is3d,
            canAddPlace = screenState.canAddPlace,
            selectedTab = uiState.selectedTab,
            onBackClick = onBackClick,
            onActionClick = {
                if (screenState.action == MapDetailAction.Join) viewModel.join() else viewModel.leave()
            },
            on3dToggleClick = on3dToggleClick,
            onAddPlaceClick = {
                // 시트를 닫아도 ViewModel 은 이 화면에 매여 살아남는다. 지우지 않으면
                // 다시 열었을 때 직전에 등록한 장소의 폼이 그대로 보인다.
                addPlaceViewModel.reset()
                addPlaceSheetVisible = true
            },
            onTabSelected = { tab -> uiState = uiState.selectTab(tab) },
            places = filterPlaces(SamplePlaces, uiState.selectedCategory),
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { category -> uiState = uiState.selectCategory(category) },
            onPlaceClick = { placeId -> uiState = uiState.selectPlace(placeId) },
            mapContent = {
                MapDetailMap(
                    mapViewportState = mapViewportState,
                    markers = screenState.places.map { place -> place.toPlaceMarker() },
                    is3d = is3d,
                    onMarkerClick = onMarkerClick,
                    onClusterClick = onClusterClick,
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )

        // 스낵바 자리는 하나뿐이라 두 출처를 한 줄로 모은다. 서버 실패가 먼저다.
        ErrorSnackbar(
            message = screenState.errorMessage ?: addPlaceNotice,
            onShown = {
                if (screenState.errorMessage != null) {
                    viewModel.consumeErrorMessage()
                } else {
                    addPlaceNotice = null
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    selectedPlace?.let { place ->
        PlaceDetailSheet(
            place = place,
            reviews = SamplePlaceReviews,
            onDismiss = closePlaceDetail,
        )
    }

    // 지도를 아직 못 읽었으면 열지 않는다. 버튼 글씨와 mapId 가 지도 정보에 달려 있다.
    val map = screenState.map.mapOrNull
    if (addPlaceSheetVisible && map != null) {
        AddPlaceSheet(
            map = map,
            viewModel = addPlaceViewModel,
            onDismiss = { addPlaceSheetVisible = false },
            onAdded = { message ->
                addPlaceSheetVisible = false
                addPlaceNotice = message
                // 장소 수가 늘었다. 상단과 시트 제목이 옛 값을 들고 있으면 안 된다.
                viewModel.retry()
            },
        )
    }
}

@Composable
internal fun MapDetailContent(
    mapTitle: String,
    roleBadge: String?,
    action: MapDetailAction,
    actionEnabled: Boolean,
    placeCount: Int?,
    is3d: Boolean,
    canAddPlace: Boolean,
    selectedTab: MapDetailTab,
    onBackClick: () -> Unit,
    onActionClick: () -> Unit,
    on3dToggleClick: () -> Unit,
    onAddPlaceClick: () -> Unit,
    onTabSelected: (MapDetailTab) -> Unit,
    places: List<PlaceUiModel>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onPlaceClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
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
                        selectedCategory = selectedCategory,
                        is3d = is3d,
                        canAddPlace = canAddPlace,
                        onCategorySelected = onCategorySelected,
                        onPlaceClick = onPlaceClick,
                        onTabSelected = onTabSelected,
                        on3dToggleClick = on3dToggleClick,
                        onAddPlaceClick = onAddPlaceClick,
                        mapContent = mapContent,
                    )
                }
                MapDetailTab.Logs -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "로그 부분입니다",
                            style = MoaMapTheme.typography.body1,
                            color = MoaMapTheme.colors.textNormal,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        MapDetailTabBar(
                            selectedTab = selectedTab,
                            onTabSelected = onTabSelected,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(start = 20.dp, top = 16.dp, end = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapDetailPlacesContent(
    places: List<PlaceUiModel>,
    placeCount: Int?,
    selectedCategory: String,
    is3d: Boolean,
    canAddPlace: Boolean,
    onCategorySelected: (String) -> Unit,
    onPlaceClick: (Long) -> Unit,
    onTabSelected: (MapDetailTab) -> Unit,
    on3dToggleClick: () -> Unit,
    onAddPlaceClick: () -> Unit,
    mapContent: @Composable () -> Unit,
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true,
        ),
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            MapDetailBottomSheet(
                places = places,
                placeCount = placeCount,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
                onPlaceClick = onPlaceClick,
            )
        },
        sheetPeekHeight = SheetPeekHeight,
        sheetShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp),
        sheetContainerColor = MoaMapTheme.colors.backgroundSecondary,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 10.dp,
        sheetDragHandle = null,
        modifier = Modifier.fillMaxSize(),
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            mapContent()
            MapDetailTabBar(
                selectedTab = MapDetailTab.Places,
                onTabSelected = onTabSelected,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp),
            )
            MapDetailMapControls(
                is3d = is3d,
                canAddPlace = canAddPlace,
                on3dToggleClick = on3dToggleClick,
                onAddPlaceClick = onAddPlaceClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = SheetPeekHeight + MapControlsBottomGap),
            )
        }
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
            selectedTab = MapDetailTab.Places,
            onBackClick = {},
            onActionClick = {},
            on3dToggleClick = {},
            onAddPlaceClick = {},
            onTabSelected = {},
            places = SamplePlaces,
            selectedCategory = "전체",
            onCategorySelected = {},
            onPlaceClick = {},
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
            selectedTab = MapDetailTab.Places,
            onBackClick = {},
            onActionClick = {},
            on3dToggleClick = {},
            onAddPlaceClick = {},
            onTabSelected = {},
            places = SamplePlaces,
            selectedCategory = "전체",
            onCategorySelected = {},
            onPlaceClick = {},
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
