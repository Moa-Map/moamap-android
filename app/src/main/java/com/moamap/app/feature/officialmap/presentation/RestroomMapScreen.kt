package com.moamap.app.feature.officialmap.presentation

import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.LocationPermissions
import com.moamap.app.feature.mapdetail.MyLocationButton
import com.moamap.app.feature.mapdetail.ViewportBounds
import com.moamap.app.feature.mapdetail.currentLocation
import com.moamap.app.feature.mapdetail.hasLocationPermission
import com.moamap.app.feature.mapdetail.myLocationZoom
import com.moamap.app.feature.mapdetail.presentation.MapDetailViewModel
import com.moamap.app.feature.officialmap.domain.model.RestroomMarker
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.coroutine.mapIdleEvents
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings

private const val RESTROOM_LAYER_ID = "restroom-circle"
private const val RESTROOM_ID_PROPERTY = "id"

/** 처음 보는 자리. 서울 시청 앞. */
private val RestroomMapStart = Point.fromLngLat(126.9780, 37.5665)

/** 처음 줌. 시청 근처 화장실이 한도(500)를 넘지 않고 마커가 겹치지 않는 정도. */
private const val RestroomMapZoom = 15.0

/** 마커가 작아 손끝이 빗나가기 쉽다. 이만큼 떨어져 눌러도 그 마커로 본다. */
private const val RestroomTapRadius = 12.0

/**
 * 공중화장실 지도. 장소가 없는 특수 공식지도라 지도 상세 대신 이 화면이 열린다.
 * 상단바·참여·나가기는 [OfficialMapScaffold] 가 지도 상세 것을 그대로 쓴다.
 *
 * 시안이 아직 없어 유동인구 화면과 같은 틀로 그린다. 마커는 파란 점, 누르면 아래에 정보 카드.
 *
 * @param initialTitle 서버 이름이 오기 전까지 상단바를 채우는 값.
 */
@Composable
fun RestroomMapScreen(
    onBackClick: () -> Unit,
    initialTitle: String,
    modifier: Modifier = Modifier,
    viewModel: RestroomMapViewModel = hiltViewModel(),
    membershipViewModel: MapDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OfficialMapScaffold(
        initialTitle = initialTitle,
        onBackClick = onBackClick,
        membershipViewModel = membershipViewModel,
        modifier = modifier,
    ) {
        RestroomMapBody(
            uiState = uiState,
            onCameraIdle = viewModel::onCameraIdle,
            onRestroomClick = viewModel::selectRestroom,
            onMapClick = viewModel::clearSelection,
        )
    }
}

/**
 * 참여 전 공식 상세의 작은 지도. 화면과 같은 마커를 그리기만 하고 누르는 동작은 없다.
 *
 * 소개 화면의 백스택 항목에 따로 ViewModel 을 둔다. 화장실 화면으로 넘어가면 그쪽이 다시 읽는다.
 */
@Composable
fun RestroomPreviewMap(
    modifier: Modifier = Modifier,
    viewModel: RestroomMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RestroomMarkersMap(
        restrooms = uiState.restrooms,
        selected = null,
        onCameraIdle = viewModel::onCameraIdle,
        modifier = modifier,
    )
}

@Composable
private fun RestroomMapBody(
    uiState: RestroomMapUiState,
    onCameraIdle: (ViewportBounds) -> Unit,
    onRestroomClick: (Long) -> Unit,
    onMapClick: () -> Unit,
) {
    val context = LocalContext.current
    val mapViewportState = rememberRestroomViewportState()

    // 내 위치 이동은 지도 상세와 같은 방식이다. 다만 들어올 때 권한을 묻지 않는다 - 시작 자리가
    // 내 위치가 아니라 시청이라, 버튼을 누를 때 물으면 된다.
    var locationGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    var myLocationRequest by remember { mutableIntStateOf(0) }
    var myLocationInProgress by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        locationGranted = result.values.any { isGranted -> isGranted }
        if (!locationGranted) notice = "위치 권한이 없어 현재 위치를 찾을 수 없어요"
    }

    // 버튼은 요청만 올리고 옮기는 일은 여기서 한다. 권한을 묻느라 미뤄진 요청도 허용되면 이어진다.
    LaunchedEffect(myLocationRequest, locationGranted) {
        if (myLocationRequest == 0 || !locationGranted) return@LaunchedEffect

        myLocationInProgress = true
        val point = try {
            currentLocation()
        } finally {
            myLocationInProgress = false
        }
        if (point == null) {
            notice = "현재 위치를 찾지 못했어요"
            return@LaunchedEffect
        }
        mapViewportState.easeTo(
            cameraOptions {
                center(point)
                zoom(myLocationZoom(mapViewportState.cameraState?.zoom))
            },
            MapAnimationOptions.mapAnimationOptions { duration(600L) },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RestroomMarkersMap(
            restrooms = uiState.restrooms,
            selected = uiState.selected,
            onCameraIdle = onCameraIdle,
            onRestroomClick = onRestroomClick,
            onMapClick = onMapClick,
            mapViewportState = mapViewportState,
            modifier = Modifier.fillMaxSize(),
        )

        val mapMessage = when {
            uiState.loadFailed -> "화장실 정보를 불러오지 못했어요. 지도를 움직이면 다시 불러와요"
            uiState.truncated -> "화장실이 많아 일부만 보여요. 지도를 확대해 주세요"
            else -> null
        }
        mapMessage?.let { message ->
            RestroomMapNotice(
                text = message,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp),
            )
        }

        // 카드가 뜨면 내 위치 버튼은 카드 위로 올라간다.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MyLocationButton(
                inProgress = myLocationInProgress,
                onClick = {
                    if (!myLocationInProgress) {
                        myLocationRequest++
                        if (!locationGranted) permissionLauncher.launch(LocationPermissions)
                    }
                },
            )
            uiState.selected?.let { restroom ->
                RestroomInfoCard(
                    restroom = restroom,
                    detail = uiState.detail,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        ErrorSnackbar(
            message = notice,
            onShown = { notice = null },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun rememberRestroomViewportState(): MapViewportState = rememberMapViewportState {
    setCameraOptions {
        center(RestroomMapStart)
        zoom(RestroomMapZoom)
    }
}

/**
 * 화장실 마커 지도. 카메라가 멈출 때마다 보이는 범위를 [onCameraIdle] 로 알린다.
 *
 * 마커는 많게는 500개라 지도 상세처럼 마커마다 뷰를 띄우지 않고 원 레이어 하나로 그린다.
 * 회전·기울기는 닫는다 - 화장실을 찾는 데 쓸 일이 없고, 기울이면 먼 곳까지 범위에 들어간다.
 */
@OptIn(MapboxDelicateApi::class)
@Composable
private fun RestroomMarkersMap(
    restrooms: List<RestroomMarker>,
    selected: RestroomMarker?,
    onCameraIdle: (ViewportBounds) -> Unit,
    modifier: Modifier = Modifier,
    onRestroomClick: (Long) -> Unit = {},
    onMapClick: () -> Unit = {},
    mapViewportState: MapViewportState = rememberRestroomViewportState(),
) {
    val mapState = rememberMapState {
        gesturesSettings = GesturesSettings {
            rotateEnabled = false
            pitchEnabled = false
        }
    }

    val restroomFeatures = remember(restrooms) { restrooms.map { it.toFeature() } }
    val selectedFeatures = remember(selected) { listOfNotNull(selected?.toFeature()) }
    val restroomSource = rememberGeoJsonSourceState { data = GeoJSONData(restroomFeatures) }
    val selectedSource = rememberGeoJsonSourceState { data = GeoJSONData(selectedFeatures) }
    // 같은 값을 다시 넣으면 GeoJSONData 의 equals 로 무시된다.
    restroomSource.data = GeoJSONData(restroomFeatures)
    selectedSource.data = GeoJSONData(selectedFeatures)

    // 상호작용은 처음 한 번만 걸고, 콜백은 늘 최신 값을 보게 한다.
    val currentOnRestroomClick by rememberUpdatedState(onRestroomClick)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)
    val standardStyleState = rememberStandardStyleState {
        configurationsState.lightPreset = LightPresetValue.DAY
        configurationsState.show3dObjects = BooleanValue(false)
        interactionsState.onLayerClicked(id = RESTROOM_LAYER_ID, radius = RestroomTapRadius) { feature, _ ->
            val id = feature.properties.optLong(RESTROOM_ID_PROPERTY, -1L)
            if (id >= 0) currentOnRestroomClick(id)
            true
        }
        // 마커가 받은 탭은 여기까지 오지 않는다. 빈 곳을 눌렀을 때만 온다.
        interactionsState.onMapClicked { _ ->
            currentOnMapClick()
            true
        }
    }

    MapboxMap(
        modifier = modifier,
        mapViewportState = mapViewportState,
        mapState = mapState,
        compass = {},
        scaleBar = {},
        style = { MapboxStandardStyle(standardStyleState = standardStyleState) },
    ) {
        MapEffect(Unit) { mapView ->
            val map = mapView.mapboxMap
            map.mapIdleEvents.collect {
                val bounds = map.coordinateBoundsForRect(
                    RectF(0f, 0f, mapView.width.toFloat(), mapView.height.toFloat()),
                )
                currentOnCameraIdle(
                    ViewportBounds(
                        west = bounds.west(),
                        south = bounds.south(),
                        east = bounds.east(),
                        north = bounds.north(),
                    ),
                )
            }
        }

        CircleLayer(sourceState = restroomSource, layerId = RESTROOM_LAYER_ID) {
            circleColor = ColorValue(MoaMapPrimitiveColors.Blue500)
            circleRadius = DoubleValue(7.0)
            circleStrokeColor = ColorValue(MoaMapPrimitiveColors.White)
            circleStrokeWidth = DoubleValue(2.0)
            // 지도 조명에 따라 색이 어두워지지 않게 한다.
            circleEmissiveStrength = DoubleValue(1.0)
        }
        CircleLayer(sourceState = selectedSource, layerId = "restroom-selected-circle") {
            circleColor = ColorValue(MoaMapPrimitiveColors.Blue800)
            circleRadius = DoubleValue(11.0)
            circleStrokeColor = ColorValue(MoaMapPrimitiveColors.White)
            circleStrokeWidth = DoubleValue(3.0)
            circleEmissiveStrength = DoubleValue(1.0)
        }
    }
}

private fun RestroomMarker.toFeature(): Feature =
    Feature.fromGeometry(Point.fromLngLat(longitude, latitude)).apply {
        addNumberProperty(RESTROOM_ID_PROPERTY, id)
    }

/** 지도 위 안내 한 줄. 상태가 풀릴 때까지 떠 있다. */
@Composable
private fun RestroomMapNotice(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MoaMapTheme.typography.body3,
        color = MoaMapPrimitiveColors.White,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(MoaMapPrimitiveColors.Blue800, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
