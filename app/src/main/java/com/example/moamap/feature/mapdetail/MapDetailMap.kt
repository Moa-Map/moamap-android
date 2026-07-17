package com.example.moamap.feature.mapdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState

@Composable
internal fun MapDetailMap(
    mapViewportState: MapViewportState,
    modifier: Modifier = Modifier,
) {
    MapboxMap(
        modifier = modifier,
        mapViewportState = mapViewportState,
    )
}
