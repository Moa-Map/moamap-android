package com.example.moamap.feature.explore.presentation

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
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors

/** 대표 이미지가 없을 때 로고가 차지하는 비율. */
private const val PLACEHOLDER_LOGO_WIDTH_RATIO = 0.47f

/**
 * 지도 카드의 대표 이미지.
 *
 * `imageUrl` 이 없으면 Blue50 바탕에 모아맵 로고를 얹은 기본 이미지를 그린다.
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
            .clip(RoundedCornerShape(12.dp))
            .background(MoaMapPrimitiveColors.Blue50)
            .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl == null) {
            Image(
                painter = painterResource(R.drawable.img_moa_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(PLACEHOLDER_LOGO_WIDTH_RATIO)
                    .padding(vertical = 4.dp),
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}
