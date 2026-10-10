package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors

/**
 * 시트 맨 위 손잡이. 시안 「Grabber」 회색 알약 35×5(#CCCCCC 자리에 같은 토큰).
 *
 * 놓을 자리(위 여백·감싸는 상자)는 시트마다 달라 부르는 쪽이 정한다.
 *
 * @param width 장소 시트만 시안이 36 이다.
 */
@Composable
internal fun MoaMapSheetGrabber(
    modifier: Modifier = Modifier,
    width: Dp = 35.dp,
) {
    Box(
        modifier = modifier
            .size(width = width, height = 5.dp)
            .background(color = MoaMapPrimitiveColors.Gray100, shape = CircleShape),
    )
}
