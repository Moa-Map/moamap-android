package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors

private val TooltipShape = RoundedCornerShape(12.dp)
private val TooltipTailWidth = 16.dp
private val TooltipTailHeight = 12.dp

/** 말풍선 상자가 꼬리 아래쪽을 덮는 만큼. 시안에서 꼬리 12 중 위 8 만 보인다. */
private val TooltipTailOverlap = 4.dp

/**
 * 위를 가리키는 꼬리가 달린 안내 말풍선. 시안 Blue800·모서리 12·패딩 16.
 *
 * 꼬리는 [tailAlignment] 쪽 끝에서 [tailInset] 만큼 들어간 자리에 선다. 시안의 그림자와
 * 꼬리 둥근 끝은 그리지 않는다 - 멤버 관리 말풍선 때 정한 그대로다.
 */
@Composable
fun MoaMapTooltip(
    tailAlignment: Alignment.Horizontal,
    tailInset: Dp,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = tailAlignment,
    ) {
        Canvas(
            modifier = Modifier
                .padding(horizontal = tailInset)
                .size(TooltipTailWidth, TooltipTailHeight),
        ) {
            val tail = Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path = tail, color = MoaMapPrimitiveColors.Blue800)
        }
        Row(
            modifier = Modifier
                .offset(y = -TooltipTailOverlap)
                .clip(TooltipShape)
                .background(MoaMapPrimitiveColors.Blue800)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/**
 * 기준 요소 바로 아래에 팝업을 띄운다.
 *
 * [alignEnd] 면 팝업 오른쪽 끝을, 아니면 왼쪽 끝을 기준 요소에 맞춘 뒤 [offset] 만큼 옮긴다.
 */
class BelowAnchorPosition(
    private val offset: IntOffset,
    private val alignEnd: Boolean = false,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = if (alignEnd) anchorBounds.right - popupContentSize.width else anchorBounds.left
        return IntOffset(x + offset.x, anchorBounds.bottom + offset.y)
    }
}
