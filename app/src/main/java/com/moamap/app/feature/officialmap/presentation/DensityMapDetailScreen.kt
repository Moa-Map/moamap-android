package com.moamap.app.feature.officialmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.MapDetailTopBar
import com.moamap.app.feature.mapdetail.MapLeaveDialog
import com.moamap.app.feature.mapdetail.domain.model.MapDetailAction
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
 *
 * 참여·나가기는 지도 상세와 같다. 같은 지도 번호로 지도 상세의 [MapDetailViewModel] 을 그대로
 * 쓰고, 상단바·나가기 팝업도 지도 상세 것을 쓴다 - 공식지도는 메뉴 없이 참여하기/나가기 글자다.
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
    val membership by membershipViewModel.uiState.collectAsStateWithLifecycle()
    // 나가기는 바로 하지 않고 이 팝업에서 한 번 더 묻는다.
    var leaveDialogVisible by rememberSaveable { mutableStateOf(false) }
    val title = membership.title ?: initialTitle

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundSecondary)
                .statusBarsPadding(),
        ) {
            MapDetailTopBar(
                mapTitle = title,
                roleBadge = membership.roleBadge,
                action = membership.action,
                actionEnabled = !membership.actionInProgress,
                onBackClick = onBackClick,
                onActionClick = {
                    if (membership.action == MapDetailAction.Join) {
                        membershipViewModel.join()
                    } else {
                        leaveDialogVisible = true
                    }
                },
            )

            DensityMapBody(
                uiState = uiState,
                onRetryClick = viewModel::retry,
                onAreaClick = viewModel::selectArea,
                onLevelClick = viewModel::selectLevel,
            )
        }

        ErrorSnackbar(
            message = membership.errorMessage,
            onShown = membershipViewModel::consumeErrorMessage,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // 나갈 수 없는 상태가 되면(나가기를 마쳐 참여가 풀리면) 같이 닫힌다.
    val leaveOutcome = membership.leaveOutcome
    if (leaveDialogVisible && leaveOutcome != null) {
        MapLeaveDialog(
            mapName = title,
            outcome = leaveOutcome,
            onConfirm = {
                leaveDialogVisible = false
                membershipViewModel.leave()
            },
            onDismiss = { leaveDialogVisible = false },
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
                        onClick = onRetryClick,
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text(text = "다시 시도")
                    }
                }
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
