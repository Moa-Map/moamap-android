package com.example.moamap.feature.mapdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val MarkerPhotoSize = 56.dp
private val MarkerRingWidth = 3.dp
private val MarkerTailWidth = 12.dp
private val MarkerTailHeight = 8.dp
private val MarkerElevation = 6.dp
private val FacepileAvatarSize = 40.dp
private val FacepileOverlap = 14.dp

/** 사진 자리를 채우는 대체 그림. 로드 전·실패·URL 없음을 한 모양으로 다룬다. */
@Composable
private fun AvatarPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(MoaMapPrimitiveColors.Yellow50),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_photo_camera),
            contentDescription = null,
            tint = MoaMapPrimitiveColors.Gray500,
            modifier = Modifier.size(20.dp),
        )
    }
}

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
        modifier = modifier.clickable(onClick = onClick),
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
            .clickable(onClick = onClick)
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
