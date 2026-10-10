package com.moamap.app.feature.mapdetail.presentation.addplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.MoaMapSheetGrabber
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.PlaceFocusZoom
import com.moamap.app.feature.mapdetail.PlaceMarker
import com.moamap.app.feature.mapdetail.PlacePhotoMarker
import com.moamap.app.feature.mapdetail.PlacePhotoMarkerHeight
import com.moamap.app.feature.mapdetail.domain.model.PlaceCandidate
import com.moamap.app.feature.mapdetail.domain.model.PlaceCategoryGroup

private val SheetShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)

/** 손잡이를 담은 시트 맨 위 줄(시안 Toolbar 56). 글은 그 아래 24 에서 시작한다. */
private val SheetToolbarHeight = 56.dp

/** 시안 버튼은 위아래 안쪽 14 라 글 줄까지 약 49 다. 등록 폼 버튼(54)보다 낮다. */
private val ConfirmButtonHeight = 49.dp

/**
 * 장소 추가 2단계. 고른 장소를 지도에서 보여 주고 맞는지 확인받는다. 시안 「미리보기」(지도에서 위치
 * 확인) `4070:38291`.
 *
 * 지도는 끌고 확대할 수 있고(사용자 결정 10-09), 마커는 고른 장소 하나뿐이다 - 지도 상세와 같은
 * 카테고리 아이콘 마커다(카카오 검색은 사진을 주지 않는다). 처음에는 장소가 아래 시트 위 빈 곳의
 * 가운데에 오게 놓는다.
 */
@Composable
internal fun PlaceLocationContent(
    candidate: PlaceCandidate,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val point = remember(candidate) { Point.fromLngLat(candidate.longitude, candidate.latitude) }
    val marker = remember(candidate) {
        PlaceMarker(
            // 지도에 아직 없는 장소라 우리 id 가 없다. 그리기에는 쓰지 않는다.
            placeId = 0L,
            name = candidate.name,
            longitude = candidate.longitude,
            latitude = candidate.latitude,
            photoUrl = null,
            categoryGroup = candidate.category?.let { path -> PlaceCategoryGroup.fromCategoryPath(path) },
        )
    }
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(point)
            zoom(PlaceFocusZoom)
        }
    }
    val styleState = rememberStandardStyleState {
        configurationsState.lightPreset = LightPresetValue.DAY
    }

    // 시트가 지도 아래쪽을 덮는다. 시트 높이를 알면 장소가 시트 위 빈 곳 가운데에 오도록 여백을 준다.
    // 마커는 좌표 위로 그려져 위 여백을 마커 높이만큼 주면 마커 몸통이 가운데다.
    var sheetHeightPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(sheetHeightPx) {
        if (sheetHeightPx == 0) return@LaunchedEffect
        mapViewportState.setCameraOptions {
            center(point)
            zoom(PlaceFocusZoom)
            padding(
                EdgeInsets(
                    with(density) { PlacePhotoMarkerHeight.toPx().toDouble() },
                    0.0,
                    sheetHeightPx.toDouble(),
                    0.0,
                ),
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            // 축척과 나침반을 띄우지 않는다. 로고와 저작권 표시는 약관상 남긴다.
            compass = {},
            scaleBar = {},
            style = { MapboxStandardStyle(standardStyleState = styleState) },
        ) {
            ViewAnnotation(
                options = viewAnnotationOptions {
                    geometry(point)
                    allowOverlap(true)
                    annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                },
            ) {
                PlacePhotoMarker(marker = marker, onClick = {})
            }
        }

        LocationSheet(
            candidate = candidate,
            onConfirmClick = onConfirmClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { size -> sheetHeightPx = size.height },
        )
    }
}

@Composable
private fun LocationSheet(
    candidate: PlaceCandidate,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SheetShape,
        color = MoaMapTheme.colors.backgroundSecondary,
        // 지도 상세 장소 목록 시트와 같은 그림자.
        shadowElevation = 10.dp,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SheetToolbarHeight),
            ) {
                // 손잡이는 모양만이다. 이 시트는 끌어 올리지 않는다.
                MoaMapSheetGrabber(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 5.dp),
                )
            }

            Column(
                modifier = Modifier.padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = candidate.name,
                        style = MoaMapTheme.typography.title3,
                        color = MoaMapTheme.colors.textNormal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (candidate.displayAddress.isNotEmpty()) {
                        Text(
                            text = candidate.displayAddress,
                            style = MoaMapTheme.typography.subtitle4,
                            color = MoaMapTheme.colors.textNormal,
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = null,
                        tint = MoaMapTheme.colors.textAssistive,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "표시된 장소가 맞는지 확인해주세요",
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapTheme.colors.textAssistive,
                    )
                }

                SubmitButton(
                    label = "이 위치로 장소 추가하기",
                    enabled = true,
                    onClick = onConfirmClick,
                    height = ConfirmButtonHeight,
                )
            }
        }
    }
}
