package com.moamap.app.feature.explore.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors

private val ThumbnailShape = RoundedCornerShape(12.dp)

/** 대표 이미지가 없을 때 로고가 차지하는 비율. */
private const val PLACEHOLDER_LOGO_WIDTH_RATIO = 0.47f

/**
 * 지도 카드의 대표 이미지.
 *
 * `imageUrl` 이 없으면 Blue50 바탕에 모아맵 로고를 얹은 기본 이미지를 그린다.
 *
 * 로고는 항상 바닥에 깔고 그 위에 대표 이미지를 덮는다. 이렇게 하면 로드가 실패하거나
 * 아직 끝나지 않았을 때 자연스럽게 기본 이미지가 드러나, 빈 배경만 남지 않는다.
 *
 * 탐색 탭 카드 전용이다(시안 「기본 이미지」 `1480:2497`). 모음 탭 카드는 시안이 다른 기본
 * 사진을 써서 [com.moamap.app.core.designsystem.component.PhotoThumbnail] 을 쓴다.
 */
@Composable
fun MapThumbnail(
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(ThumbnailShape)
            .background(MoaMapPrimitiveColors.Blue50)
            .border(1.dp, Color.Black.copy(alpha = 0.2f), ThumbnailShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_moa_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(PLACEHOLDER_LOGO_WIDTH_RATIO)
                .padding(vertical = 4.dp),
        )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}
