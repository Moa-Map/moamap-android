package com.example.moamap.feature.officialmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.officialmap.domain.model.CongestionLevel
import com.example.moamap.feature.officialmap.domain.model.DensityArea
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

@Composable
fun DensityMapDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DensityMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        DensityMapTopBar(
            mapTitle = "실시간 유동인구 지도",
            onBackClick = onBackClick,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is DensityMapUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is DensityMapUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "밀집도 정보를 불러오지 못했어요",
                            style = MoaMapTheme.typography.body1,
                            color = MoaMapTheme.colors.textNormal,
                        )
                        Button(
                            onClick = viewModel::retry,
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Text(text = "다시 시도")
                        }
                    }
                }

                is DensityMapUiState.Success -> {
                    DensityMapContent(
                        areas = state.areas,
                        selectedArea = state.areas.firstOrNull { it.code == state.selectedCode },
                        onAreaClick = viewModel::selectArea,
                    )
                }
            }
        }
    }
}

@OptIn(MapboxDelicateApi::class)
@Composable
private fun DensityMapContent(
    areas: List<DensityArea>,
    selectedArea: DensityArea?,
    onAreaClick: (String?) -> Unit,
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

    val standardStyleState = rememberStandardStyleState()
    // 폴리곤 탭 → 지역 코드 추출 후 선택 토글
    standardStyleState.interactionsState.onLayerClicked(id = DENSITY_FILL_LAYER_ID) { feature, _ ->
        val code = feature.properties.optString("code")
        if (code.isNotEmpty()) onAreaClick(code)
        true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = {
                MapboxStandardStyle(standardStyleState = standardStyleState)
            },
        ) {
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

        CongestionLegend(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
        )

        selectedArea?.let { area ->
            AreaInfoChip(
                area = area,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
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
