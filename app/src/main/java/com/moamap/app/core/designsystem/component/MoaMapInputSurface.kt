package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors

/** 입력하고 있는 칸 테두리. 시안 「프로필 편집 화면」(이름 칸 입력 중): Blue300 1px. */
private val FocusedInputBorder = BorderStroke(1.dp, MoaMapPrimitiveColors.Blue300)

/**
 * 입력 칸 바탕. [ShadowedSurface] 에, 안에서 입력하고 있으면 하늘색 테두리를 두른다(10-05 사용자 결정 -
 * 흰 칸 입력란 전체. 검색 칸·댓글 입력·초대코드는 시안대로 테두리 없음).
 *
 * 키보드를 내려도 다른 곳을 눌러 칸을 떠나기 전까지는 입력 중으로 본다. 입력이 막힌 칸은 선택되지
 * 않으니 켜지지 않는다. [highlighted] 는 입력란이 아닌 칸(문의 유형처럼 목록을 여는 칸)이 켤 때 쓴다.
 */
@Composable
fun MoaMapInputSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    color: Color = MoaMapPrimitiveColors.White,
    shadowBlurRadius: Dp = CardShadowBlurRadius,
    shadowColor: Color = CardShadowColor,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    ShadowedSurface(
        modifier = modifier.onFocusChanged { state -> focused = state.hasFocus },
        shape = shape,
        color = color,
        shadowBlurRadius = shadowBlurRadius,
        shadowColor = shadowColor,
        border = if (focused || highlighted) FocusedInputBorder else null,
        onClick = onClick,
        content = content,
    )
}
