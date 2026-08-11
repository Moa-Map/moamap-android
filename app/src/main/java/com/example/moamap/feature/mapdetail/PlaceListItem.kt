package com.example.moamap.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val PlaceCardShape = RoundedCornerShape(16.dp)
private val PlaceThumbnailShape = RoundedCornerShape(4.dp)
internal val PlaceListTextMinHeight = 64.dp

@Composable
internal fun PlaceListItem(
    place: PlaceUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PlaceCardShape,
        color = MoaMapPrimitiveColors.White,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(PlaceThumbnailShape)
                    .background(MoaMapPrimitiveColors.Yellow50),
                contentAlignment = Alignment.Center,
            ) {
                // 사진이 없거나 받는 중일 때 노란 자리만 남지 않게 아이콘을 깔아 둔다.
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = null,
                    tint = MoaMapPrimitiveColors.Gray500,
                    modifier = Modifier.size(24.dp),
                )
                AsyncImage(
                    model = place.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = PlaceListTextMinHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = place.name,
                        style = MoaMapTheme.typography.subtitle2,
                        color = MoaMapTheme.colors.textNormal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        painter = painterResource(
                            if (place.favorite) {
                                R.drawable.ic_favorite_filled
                            } else {
                                R.drawable.ic_favorite_outline
                            },
                        ),
                        contentDescription = if (place.favorite) "즐겨찾기" else "즐겨찾기 안 함",
                        tint = if (place.favorite) {
                            MoaMapTheme.colors.statusAlert
                        } else {
                            MoaMapPrimitiveColors.Gray100
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }

                Text(
                    text = place.description,
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapTheme.colors.textAlternative,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaceMetric(
                        iconRes = R.drawable.ic_star_filled,
                        value = place.rating.toString(),
                        contentDescription = "평점",
                        tint = MoaMapPrimitiveColors.Yellow500,
                    )
                    PlaceMetric(
                        iconRes = R.drawable.ic_comment,
                        value = place.reviewCount.toString(),
                        contentDescription = "댓글 수",
                        tint = MoaMapPrimitiveColors.Blue500,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceMetric(
    iconRes: Int,
    value: String,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = value,
            style = MoaMapTheme.typography.caption2,
            color = MoaMapTheme.colors.textAlternative,
        )
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun PlaceListItemPreview() {
    MoaMapTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MoaMapTheme.colors.backgroundSecondary)
                .padding(20.dp),
        ) {
            PlaceListItem(
                place = SamplePlaces.first(),
                onClick = {},
            )
        }
    }
}
