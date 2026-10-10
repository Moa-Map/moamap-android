package com.moamap.app.feature.officialmap.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import androidx.compose.ui.platform.LocalContext
import com.moamap.app.feature.mapdetail.MyLocationPuck
import com.moamap.app.feature.mapdetail.hasLocationPermission
import com.moamap.app.feature.mapdetail.presentation.MapDetailViewModel
import com.moamap.app.feature.officialmap.domain.model.CongestionLevel
import com.moamap.app.feature.officialmap.domain.model.DensityArea
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.generated.FillLayer
import com.mapbox.maps.extension.compose.style.layers.generated.LineLayer
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.extension.style.expressions.generated.Expression

private const val DENSITY_FILL_LAYER_ID = "density-fill"

/**
 * 유동인구 지도. 장소가 없는 특수 공식지도라 지도 상세 대신 이 화면이 열린다.
 * 상단바·참여·나가기는 [OfficialMapScaffold] 가 지도 상세 것을 그대로 쓴다.
 *
 * @param initialTitle 서버 이름이 오기 전까지 상단바를 채우는 값.
 */
@Composable
fun DensityMapDetailScreen(
    onBackClick: () -> Unit,
    initialTitle: String,
    modifier: Modifier = Modifier,
    viewModel: DensityMapViewModel = hiltViewModel(),
    membershipViewModel: MapDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    OfficialMapScaffold(
        initialTitle = initialTitle,
        onBackClick = onBackClick,
        membershipViewModel = membershipViewModel,
        modifier = modifier,
    ) {
        DensityMapBody(
            uiState = uiState,
            onRetryClick = viewModel::retry,
            onAreaClick = viewModel::selectArea,
            onLevelClick = viewModel::selectLevel,
        )
    }
}

@Composable
private fun DensityMapBody(
    uiState: DensityMapUiState,
    onRetryClick: () -> Unit,
    onAreaClick: (String?) -> Unit,
    onLevelClick: (CongestionLevel?) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is DensityMapUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is DensityMapUiState.Error -> {
                MoaMapErrorNotice(
                    message = "밀집도 정보를 불러오지 못했어요",
                    onRetryClick = onRetryClick,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            is DensityMapUiState.Success -> {
                DensityMapContent(
                    areas = uiState.visibleAreas,
                    selectedArea = uiState.selectedArea,
                    filterLevel = uiState.filterLevel,
                    onAreaClick = onAreaClick,
                    onLevelClick = onLevelClick,
                )
            }
        }
    }
}

@OptIn(MapboxDelicateApi::class)
@Composable
private fun DensityMapContent(
    areas: List<DensityArea>,
    selectedArea: DensityArea?,
    filterLevel: CongestionLevel?,
    onAreaClick: (String?) -> Unit,
    onLevelClick: (CongestionLevel?) -> Unit,
) {
    val featureCollectionJson = remember(areas) { areas.toFeatureCollectionJson() }
    val selectedFeatureJson = remember(areas, selectedArea) {
        listOfNotNull(selectedArea).toFeatureCollectionJson()
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            // 서울 전역이 보이는 초기 카메라
            center(Point.fromLngLat(126.9780, 37.5665))
            zoom(10.5)
        }
    }

    val densitySource = rememberGeoJsonSourceState {
        data = GeoJSONData(featureCollectionJson)
    }
    val selectedSource = rememberGeoJsonSourceState {
        data = GeoJSONData(selectedFeatureJson)
    }
    // 선택 변경 시 소스 갱신 (GeoJSONData는 equals 구현이 있어 동일 값 재할당은 무시된다)
    densitySource.data = GeoJSONData(featureCollectionJson)
    selectedSource.data = GeoJSONData(selectedFeatureJson)

    // 클릭 리스너는 최초 1회만 등록하고, 콜백은 항상 최신 onAreaClick을 바라보게 한다.
    val currentOnAreaClick by rememberUpdatedState(onAreaClick)
    val standardStyleState = rememberStandardStyleState {
        // 폴리곤 탭 → 지역 코드 추출 후 선택 토글
        interactionsState.onLayerClicked(id = DENSITY_FILL_LAYER_ID) { feature, _ ->
            val code = feature.properties.optString("code")
            if (code.isNotEmpty()) currentOnAreaClick(code)
            true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            compass = {},
            style = {
                MapboxStandardStyle(standardStyleState = standardStyleState)
            },
        ) {
            // 이 화면은 권한을 묻지 않는다. 이미 허용돼 있을 때만 내 위치가 보인다.
            MyLocationPuck(enabled = hasLocationPermission(LocalContext.current))
            // 평상시: 부드러운 채움 + 흐린 경계 (하이브리드의 그라데이션 느낌)
            FillLayer(sourceState = densitySource, layerId = DENSITY_FILL_LAYER_ID) {
                fillColor = ColorValue(levelColorExpression())
                fillOpacity = DoubleValue(0.35)
            }
            LineLayer(sourceState = densitySource, layerId = "density-line") {
                lineColor = ColorValue(levelColorExpression())
                lineWidth = DoubleValue(10.0)
                lineBlur = DoubleValue(8.0)
                lineOpacity = DoubleValue(0.5)
            }
            // 선택된 지역: 진한 채움 + 또렷한 테두리
            FillLayer(sourceState = selectedSource, layerId = "density-selected-fill") {
                fillColor = ColorValue(levelColorExpression())
                fillOpacity = DoubleValue(0.55)
            }
            LineLayer(sourceState = selectedSource, layerId = "density-selected-line") {
                lineColor = ColorValue(levelColorExpression())
                lineWidth = DoubleValue(2.5)
            }
        }

        CongestionFilterChips(
            selectedLevel = filterLevel,
            onLevelClick = onLevelClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(top = 18.dp),
        )

        selectedArea?.let { area ->
            AreaInfoCard(
                area = area,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 30.dp),
            )
        }
    }
}

private fun levelColorExpression(): Expression = Expression.match {
    get { literal("level") }
    literal(CongestionLevel.RELAXED.name)
    color(CongestionLevel.RELAXED.color.toArgb())
    literal(CongestionLevel.NORMAL.name)
    color(CongestionLevel.NORMAL.color.toArgb())
    literal(CongestionLevel.SLIGHTLY_BUSY.name)
    color(CongestionLevel.SLIGHTLY_BUSY.color.toArgb())
    literal(CongestionLevel.BUSY.name)
    color(CongestionLevel.BUSY.color.toArgb())
    color(CongestionLevel.UNKNOWN.color.toArgb())
}
