package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapTheme

private val PhotoThumbnailShape = RoundedCornerShape(8.dp)

/**
 * 지도·장소·사용자 사진 칸. 시안 디자인 시스템 「이미지 플레이스홀더」 - 사각형(모서리 8)과 원형([shape]
 * 에 `CircleShape`), 둘 다 연회색 1px 테두리.
 *
 * 사진이 없거나, 받는 중이거나, 받지 못하면 시안의 기본 사진(연회색 바탕에 모아맵 로고)을 꽉 채워
 * 그린다. 사진이 있어도 같은 칸·테두리다 - 시안에서 이 칸이 곧 사진 자리다.
 *
 * @param bordered 연회색 테두리를 그릴지. 바깥에 이미 테두리가 있는 자리(댓글 작성자 원)나 그림자 원이
 * 테두리 노릇을 하는 자리(프로필 편집)에서는 뺀다.
 */
@Composable
fun PhotoThumbnail(
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = PhotoThumbnailShape,
    bordered: Boolean = true,
    contentDescription: String? = null,
) {
    val placeholder = painterResource(R.drawable.img_placeholder_logo)
    AsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(
                if (bordered) Modifier.border(1.dp, MoaMapTheme.colors.lineAlternative, shape) else Modifier,
            ),
    )
}
