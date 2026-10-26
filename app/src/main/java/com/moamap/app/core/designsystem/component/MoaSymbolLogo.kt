package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moamap.app.R

/**
 * 그림(110 x 95.75) 안의 로고 본체(108.5 x 93). 그림자가 오른쪽·아래로 번져 그림이 더 크고,
 * 본체는 그림 안 왼쪽 1.5, 위 0 에서 시작한다.
 */
private const val ImageWidth = 110f
private const val ImageHeight = 95.75f
private const val BodyWidth = 108.5f
private const val BodyHeight = 93f
private const val BodyStartInset = 1.5f

/**
 * 피그마 로고(Group 3429). 자리는 본체 크기로만 잡고 그림자는 바깥으로 번지게 둔다(iOS `MoaSymbolLogo`).
 * 스플래시·로그인과 장소 검색 안내가 쓴다.
 *
 * @param bodyWidth 시안의 로고 본체 너비. 높이는 본체 비율로 따라간다.
 * @param contentDescription 꾸밈으로만 쓰면 null.
 */
@Composable
fun MoaSymbolLogo(
    bodyWidth: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = "모아맵",
) {
    val scale = bodyWidth.value / BodyWidth
    val imageWidth = (ImageWidth * scale).dp
    val imageHeight = (ImageHeight * scale).dp
    val bodyHeight = (BodyHeight * scale).dp
    Box(modifier = modifier.size(width = bodyWidth, height = bodyHeight)) {
        Image(
            painter = painterResource(R.drawable.img_moa_symbol_large),
            contentDescription = contentDescription,
            // requiredSize 는 칸보다 큰 그림을 칸 가운데에 둔다. 본체 왼쪽 위가 칸 왼쪽 위에 오도록 옮긴다.
            modifier = Modifier
                .requiredSize(width = imageWidth, height = imageHeight)
                .offset(
                    x = (imageWidth - bodyWidth) / 2 - (BodyStartInset * scale).dp,
                    y = (imageHeight - bodyHeight) / 2,
                ),
        )
    }
}
