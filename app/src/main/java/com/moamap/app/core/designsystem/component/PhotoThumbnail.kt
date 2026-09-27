package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R

private val PhotoThumbnailShape = RoundedCornerShape(4.dp)

/**
 * 지도·장소 사진 칸. 시안: 모음 카드 `1974:7357`, 장소 목록 `1841:13009`, 장소 상세 `1841:11100`.
 *
 * 사진이 없거나, 받는 중이거나, 받지 못하면 시안의 기본 사진을 꽉 채워 그린다. 기본 사진은
 * 반투명이라 바탕색을 깔지 않는다 - 시안처럼 뒤 배경이 비쳐야 한다.
 */
@Composable
fun PhotoThumbnail(
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val placeholder = painterResource(R.drawable.img_photo_placeholder)
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(PhotoThumbnailShape),
    )
}
