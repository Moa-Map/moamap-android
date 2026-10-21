package com.moamap.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import dev.chrisbanes.haze.HazeState

private val TooltipShape = RoundedCornerShape(12.dp)
private val TooltipTailWidth = 16.dp

/**
 * 꼬리에서 상자 위로 드러나는 높이. 시안 꼬리는 16×12 인데 상자가 아래 4 를 덮어 위 8 만 보인다.
 * 보이는 부분만 그린다 - 반투명 바탕이면 겹친 곳이 두 겹이라 진한 띠가 생긴다(10-09 사용자 결정).
 */
private val TooltipTailVisibleHeight = 8.dp

/** 꼬리 끝에서 [TooltipTailVisibleHeight] 아래까지의 삼각형. 밑변 폭은 16×12 삼각형의 그 높이 단면이다. */
private val TooltipTailShape = GenericShape { size, _ ->
    val halfBase = size.width / 2 * (8f / 12f)
    moveTo(size.width / 2, 0f)
    lineTo(size.width / 2 + halfBase, size.height)
    lineTo(size.width / 2 - halfBase, size.height)
    close()
}

/**
 * 위를 가리키는 꼬리가 달린 안내 말풍선. 모서리 12·패딩 16·칸 사이 10.
 *
 * 꼬리는 [tailAlignment] 쪽 끝에서 [tailInset] 만큼 들어간 자리에 선다. 시안의 그림자와
 * 꼬리 둥근 끝은 그리지 않는다 - 멤버 관리 말풍선 때 정한 그대로다.
 *
 * 바탕은 [color] 한 겹이다. [hazeState] 를 주면 그 원본을 흐려 깐다([modalScrim]) - 멤버 관리의
 * 회색 반투명 말풍선이 그렇다.
 */
@Composable
fun MoaMapTooltip(
    tailAlignment: Alignment.Horizontal,
    tailInset: Dp,
    modifier: Modifier = Modifier,
    color: Color = MoaMapPrimitiveColors.Blue800,
    hazeState: HazeState? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = tailAlignment,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = tailInset)
                .size(TooltipTailWidth, TooltipTailVisibleHeight)
                .clip(TooltipTailShape)
                .modalScrim(hazeState, color),
        )
        Row(
            modifier = Modifier
                .clip(TooltipShape)
                .modalScrim(hazeState, color)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/** 안내 말풍선 꼬리가 ⓘ(20) 가운데를 가리키도록 말풍선을 안내 줄 왼쪽 끝에서 당기는 거리와 꼬리 자리, 안내 줄과의 틈. */
private val GuideTooltipOffsetX = (-12).dp
private val GuideTooltipTailInset = 14.dp
private val GuideTooltipGap = 3.dp

/**
 * 「ⓘ ○○이 궁금하신가요?」 안내 줄. 누르면 바로 아래에 꼬리가 ⓘ 를 가리키는 말풍선이 뜨고,
 * 다시 누르거나 바깥·뒤로가기로 닫힌다. 멤버 관리 역할 안내와 공식지도 안내가 쓴다.
 *
 * 말풍선 바탕은 [tooltipColor]·[tooltipHazeState] 로 [MoaMapTooltip] 에 넘긴다.
 */
@Composable
fun MoaMapGuideRow(
    text: String,
    modifier: Modifier = Modifier,
    tooltipColor: Color = MoaMapPrimitiveColors.Blue800,
    tooltipHazeState: HazeState? = null,
    tooltipContent: @Composable RowScope.() -> Unit,
) {
    var guideVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val position = remember(density) {
        with(density) {
            BelowAnchorPosition(IntOffset(GuideTooltipOffsetX.roundToPx(), GuideTooltipGap.roundToPx()))
        }
    }

    // 말풍선은 이 Box 를 기준으로 자리를 잡는다.
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.clickable(role = Role.Button) { guideVisible = !guideVisible },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = MoaMapTheme.colors.textAssistive,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                color = MoaMapTheme.colors.textAssistive,
            )
        }
        if (guideVisible) {
            Popup(
                popupPositionProvider = position,
                onDismissRequest = { guideVisible = false },
                properties = PopupProperties(focusable = true),
            ) {
                MoaMapTooltip(
                    tailAlignment = Alignment.Start,
                    tailInset = GuideTooltipTailInset,
                    color = tooltipColor,
                    hazeState = tooltipHazeState,
                    content = tooltipContent,
                )
            }
        }
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
