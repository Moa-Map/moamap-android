package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

private val MapDetailTabShape = RoundedCornerShape(100.dp)

/** 상단바 아래 → 세그먼트. */
private val MapDetailTabBarTopGap = 12.dp

/** 선택 안 된 칸의 폭. 시안이 이 칸만 171 로 고정하고, 선택된 칸이 나머지를 쓴다. */
private val UnselectedTabWidth = 171.dp

/**
 * 지도 상세 장소·로그 세그먼트. 시안 「모음 Segment」(`3468:17579`).
 *
 * 좌우 24·상단바 아래 12 를 여기서 준다. 부르는 쪽은 위 가운데에 앉히기만 한다.
 * 그림자는 없다.
 */
@Composable
internal fun MapDetailTabBar(
    selectedTab: MapDetailTab,
    onTabSelected: (MapDetailTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(start = 24.dp, top = MapDetailTabBarTopGap, end = 24.dp)
            .fillMaxWidth()
            .clip(MapDetailTabShape)
            .background(MoaMapPrimitiveColors.Yellow50)
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MapDetailTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            Box(
                modifier = Modifier
                    .then(if (selected) Modifier.weight(1f) else Modifier.width(UnselectedTabWidth))
                    .clip(MapDetailTabShape)
                    .background(
                        if (selected) {
                            MoaMapPrimitiveColors.Yellow100
                        } else {
                            MoaMapPrimitiveColors.Yellow50
                        },
                    )
                    .selectable(
                        selected = selected,
                        onClick = { onTabSelected(tab) },
                        role = Role.Tab,
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    style = if (selected) {
                        MoaMapTheme.typography.button0
                    } else {
                        MoaMapTheme.typography.button1
                    },
                    color = if (selected) {
                        MoaMapPrimitiveColors.Yellow900
                    } else {
                        MoaMapTheme.colors.textAssistive
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun MapDetailTabBarPreview() {
    MoaMapTheme {
        MapDetailTabBar(selectedTab = MapDetailTab.Places, onTabSelected = {})
    }
}
