package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

private val ClusterSheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val ClusterSheetGrabberShape = RoundedCornerShape(100.dp)

/**
 * 목록이 길어져도 시트가 화면을 다 먹지 않게 잡는 상한.
 *
 * 한 자리에 수십 곳이 겹치는 일은 없지만, 낮은 줌에서는 임계값 안에 든 장소가 모두 한
 * 묶음이 되므로 개수가 얼마든 커질 수 있다.
 */
private val ClusterListMaxHeight = 420.dp

/**
 * 묶음 마커를 펼친 장소 목록.
 *
 * 묶음을 푸는 수단이 줌뿐이면 좌표가 같은 장소는 열 방법이 없다. 확대해도 화면 거리가
 * 0이라 갈라지지 않기 때문이다. 여기서 골라 열면 줌과 좌표에 기대지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClusterPlacesSheet(
    places: List<PlaceUiModel>,
    onPlaceClick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = ClusterSheetShape,
        containerColor = MoaMapTheme.colors.backgroundSecondary,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(25.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 35.dp, height = 5.dp)
                        .background(
                            color = MoaMapPrimitiveColors.Gray100,
                            shape = ClusterSheetGrabberShape,
                        ),
                )
            }
        },
    ) {
        Text(
            text = "이 위치의 장소 ${places.size}곳",
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.heightIn(max = ClusterListMaxHeight),
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(places, key = { place -> place.id }) { place ->
                PlaceListItem(
                    place = place,
                    onClick = { onPlaceClick(place.id) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun ClusterPlacesSheetPreview() {
    MoaMapTheme {
        ClusterPlacesSheet(
            places = SamplePlaces.take(2),
            onPlaceClick = {},
            onDismiss = {},
        )
    }
}
