package com.moamap.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ButtonShadowColor
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

private val BottomBarShape = RoundedCornerShape(100.dp)

/**
 * 콘텐츠 위에 떠 있는 알약형 바텀 네비게이션. 시안 「NavigationBar」(새 홈 시안 기준):
 * 그림자 0 0 10 10%, 고른 칸은 노랑 바탕에 진한 노랑 글자.
 */
enum class MoaMapBottomBarItem(
    val label: String,
    val route: MoaMapRoute,
) {
    Explore("탐색", MoaMapRoute.Explore),
    Collection("모음", MoaMapRoute.Collection),
}

@Composable
fun MoaMapBottomBar(
    currentRoute: String?,
    onItemClick: (MoaMapRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier,
        shape = BottomBarShape,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ButtonShadowColor,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MoaMapBottomBarItem.entries.forEach { item ->
                BottomBarTab(
                    item = item,
                    selected = currentRoute == item.route.route,
                    onClick = { onItemClick(item.route) },
                )
            }
        }
    }
}

@Composable
private fun BottomBarTab(
    item: MoaMapBottomBarItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(50.dp)
            .clip(BottomBarShape)
            .background(if (selected) MoaMapPrimitiveColors.Yellow200 else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.label,
            style = if (selected) MoaMapTheme.typography.button0 else MoaMapTheme.typography.button1,
            color = if (selected) MoaMapPrimitiveColors.Yellow800 else MoaMapTheme.colors.textAssistive,
        )
    }
}

@Preview
@Composable
private fun MoaMapBottomBarPreview() {
    MoaMapTheme {
        MoaMapBottomBar(
            currentRoute = MoaMapRoute.Explore.route,
            onItemClick = {},
        )
    }
}
