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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

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
    mapTitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    roleLabel: String = "방장",
    bookmarked: Boolean = true,
    onBookmarkClick: () -> Unit = {},
) {
    var uiState by rememberSaveable(stateSaver = MapDetailUiStateSaver) {
        mutableStateOf(MapDetailUiState())
    }
    val selectedPlace = SamplePlaces.firstOrNull { place ->
        place.id == uiState.selectedPlaceId
    }
    val closePlaceDetail = { uiState = uiState.closePlaceDetail() }
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(126.9574, 37.4963))
            zoom(16.5)
            bearing(0.0)
            pitch(0.0)
        }
    }

    MapDetailContent(
        mapTitle = mapTitle,
        roleLabel = roleLabel,
        bookmarked = bookmarked,
        selectedTab = uiState.selectedTab,
        onBackClick = onBackClick,
        onBookmarkClick = onBookmarkClick,
        onTabSelected = { tab -> uiState = uiState.selectTab(tab) },
        places = filterPlaces(SamplePlaces, uiState.selectedCategory),
        selectedCategory = uiState.selectedCategory,
        onCategorySelected = { category -> uiState = uiState.selectCategory(category) },
        onPlaceClick = { placeId -> uiState = uiState.selectPlace(placeId) },
        modifier = modifier,
        mapContent = {
            MapDetailMap(
                mapViewportState = mapViewportState,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )

    selectedPlace?.let { place ->
        PlaceDetailSheet(
            place = place,
            reviews = SamplePlaceReviews,
            onDismiss = closePlaceDetail,
        )
    }
}

@Composable
internal fun MapDetailContent(
    mapTitle: String,
    roleLabel: String,
    bookmarked: Boolean,
    selectedTab: MapDetailTab,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
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
            roleLabel = roleLabel,
            bookmarked = bookmarked,
            onBackClick = onBackClick,
            onBookmarkClick = onBookmarkClick,
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
                        selectedCategory = selectedCategory,
                        onCategorySelected = onCategorySelected,
                        onPlaceClick = onPlaceClick,
                        onTabSelected = onTabSelected,
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
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onPlaceClick: (Long) -> Unit,
    onTabSelected: (MapDetailTab) -> Unit,
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
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
                onPlaceClick = onPlaceClick,
            )
        },
        sheetPeekHeight = 283.dp,
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
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapDetailScreenPreview() {
    MoaMapTheme {
        MapDetailContent(
            mapTitle = "서울 데이트 지도",
            roleLabel = "방장",
            bookmarked = true,
            selectedTab = MapDetailTab.Places,
            onBackClick = {},
            onBookmarkClick = {},
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
