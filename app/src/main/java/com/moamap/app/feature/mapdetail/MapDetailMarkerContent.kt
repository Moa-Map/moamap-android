package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

private val MarkerPhotoSize = 56.dp
private val MarkerRingWidth = 3.dp
private val MarkerTailWidth = 12.dp
private val MarkerTailHeight = 8.dp
private val MarkerElevation = 6.dp
private val FacepileAvatarSize = 40.dp
private val FacepileOverlap = 14.dp

/**
 * 사진 자리를 채우는 대체 그림. 로드 전·실패·URL 없음을 한 모양으로 다룬다.
 *
 * 로고를 코드에서 자르지 않는다. moa 로고는 가로로 긴 워드마크(1.88:1)라 원형 마커에
 * 맞추려면 확대·정렬을 손으로 맞춰야 하고, 투명한 자리가 원 가장자리에 비친다. 그래서
 * 잘라 낸 결과를 정사각 불투명 에셋으로 미리 구워 두고 여기서는 그대로 깐다.
 */
@Composable
private fun AvatarPlaceholder() {
    Image(
        painter = painterResource(R.drawable.img_marker_placeholder),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape),
    )
}

/**
 * 리플 없는 클릭.
 *
 * 마커는 지도 위에 떠 있는 작은 그림이라, 기본 인디케이션을 두면 꼬리까지 포함한
 * 네모 영역에 회색이 번져 마커 모양과 어긋나 보인다. 누르면 곧바로 시트가 열리거나
 * 카메라가 움직여서, 눌렸다는 사실은 그쪽으로 이미 드러난다.
 */
@Composable
private fun Modifier.markerClickable(onClick: () -> Unit): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick,
)

@Composable
private fun AvatarCircle(
    photoUrl: String?,
    contentDescription: String?,
    size: Dp,
    ringWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MoaMapPrimitiveColors.White)
            .padding(ringWidth),
    ) {
        // 항상 뒤에 깔아 둔다. URL 이 없을 때, 받는 중일 때, 실패했을 때를 한 번에 덮는다.
        // AsyncImage 는 세 경우 모두 아무것도 그리지 않아, 없으면 흰 원만 남는다.
        AvatarPlaceholder()

        AsyncImage(
            model = photoUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
        )
    }
}

@Composable
internal fun PlacePhotoMarker(
    marker: PlaceMarker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.markerClickable(onClick),
    ) {
        AvatarCircle(
            photoUrl = marker.photoUrl,
            contentDescription = marker.name,
            size = MarkerPhotoSize,
            ringWidth = MarkerRingWidth,
            modifier = Modifier.shadow(
                elevation = MarkerElevation,
                shape = CircleShape,
                clip = false,
            ),
        )
        MarkerTail()
    }
}

/** 핀이 지면을 가리키도록 붙이는 아래쪽 삼각형 꼬리. */
@Composable
private fun MarkerTail() {
    Canvas(modifier = Modifier.size(width = MarkerTailWidth, height = MarkerTailHeight)) {
        val tail = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        drawPath(path = tail, color = MoaMapPrimitiveColors.White)
    }
}

@Composable
internal fun PlaceFacepileMarker(
    cluster: MarkerCluster,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3,
) {
    val visible = cluster.members.take(maxVisible)
    val overflow = cluster.members.size - visible.size

    Row(
        horizontalArrangement = Arrangement.spacedBy(-FacepileOverlap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(elevation = MarkerElevation, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(MoaMapPrimitiveColors.White)
            .markerClickable(onClick)
            .padding(MarkerRingWidth),
    ) {
        visible.forEachIndexed { index, member ->
            AvatarCircle(
                photoUrl = member.photoUrl,
                contentDescription = member.name,
                size = FacepileAvatarSize,
                ringWidth = 2.dp,
                modifier = Modifier.zIndex((visible.size - index).toFloat()),
            )
        }

        if (overflow > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    // 앞선 아바타들보다 항상 위에 그려지도록, 가장 높은 zIndex보다 크게 준다.
                    .zIndex((visible.size + 1).toFloat())
                    .size(FacepileAvatarSize)
                    .clip(CircleShape)
                    .background(MoaMapPrimitiveColors.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    // Gray200+White는 명암비 약 2.3:1로 WCAG AA(4.5:1) 미달이라 Gray500으로 올림.
                    .background(MoaMapPrimitiveColors.Gray500),
            ) {
                Text(
                    text = "+$overflow",
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapPrimitiveColors.White,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlacePhotoMarkerPreview() {
    MoaMapTheme {
        PlacePhotoMarker(marker = PreviewPlaceMarkers[0], onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceFacepileMarkerTwoPreview() {
    MoaMapTheme {
        PlaceFacepileMarker(
            cluster = MarkerCluster(members = PreviewPlaceMarkers),
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceFacepileMarkerOverflowPreview() {
    MoaMapTheme {
        PlaceFacepileMarker(
            cluster = MarkerCluster(
                members = PreviewPlaceMarkers + PreviewPlaceMarkers.map { marker ->
                    marker.copy(placeId = marker.placeId + 10L)
                },
            ),
            onClick = {},
        )
    }
}
