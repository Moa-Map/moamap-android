package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.presentation.intro.MapIntroPlaceItem

/**
 * 지도 상세 장소 목록 카드.
 *
 * @param showsReactions false 면 하트·설명·댓글 수 없이 이름·주소만 있는 카드를 그린다.
 *  공식지도 목록이다(시안 「공식지도 - 미리보기」의 「URL 장소」 카드).
 */
@Composable
internal fun PlaceListItem(
    place: PlaceUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLikeClick: () -> Unit = {},
    showsReactions: Boolean = true,
) {
    if (!showsReactions) {
        MapIntroPlaceItem(
            name = place.name,
            address = place.address,
            photoUrl = place.photoUrl,
            onClick = onClick,
            modifier = modifier,
        )
        return
    }

    // 시안(1841:13009) 카드. 그림자는 Material 이 아니라 피그마 값 그대로 깔아야 옅게 나온다.
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoThumbnail(imageUrl = place.photoUrl, size = 68.dp)

            // 설명은 이름 바로 아래에 붙고, 댓글 수만 12 떨어진다.
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column {
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
                        // 카드를 누르면 상세가 열린다. 하트는 따로 받아 상세로 넘어가지 않게 한다.
                        Icon(
                            painter = painterResource(likeIconRes(place.liked)),
                            contentDescription = if (place.liked) "하트 취소하기" else "하트 누르기",
                            tint = if (place.liked) {
                                MoaMapTheme.colors.statusAlert
                            } else {
                                MoaMapPrimitiveColors.Gray100
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(onClick = onLikeClick),
                        )
                    }

                    Text(
                        text = place.description,
                        style = MoaMapTheme.typography.caption0,
                        color = MoaMapTheme.colors.textNormal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // 별점은 보여주지 않는다. 후기가 고정 별점으로 쌓여 평균에 의미가 없다.
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

@Composable
private fun PlaceMetric(
    iconRes: Int,
    value: String,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
            color = MoaMapTheme.colors.textNormal,
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
