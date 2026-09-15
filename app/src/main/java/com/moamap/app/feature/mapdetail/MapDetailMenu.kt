package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ActionMenu
import com.moamap.app.core.designsystem.component.ActionMenuItem
import com.moamap.app.core.designsystem.theme.MoaMapTheme

private val MapDetailMenuCornerRadius = 12.dp

/**
 * 상단바 메뉴. 참여한 지도에서 우측 위 아이콘을 누르면 뜬다.
 *
 * @param canLeave 나가기 줄을 넣을지. 나갈 수 없는 사람에게는 줄 자체를 보여주지 않는다 -
 * `MapDetail.canLeaveFromMenu` 참고.
 */
@Composable
internal fun MapDetailMenu(
    canLeave: Boolean,
    onMembersClick: () -> Unit,
    onManageClick: () -> Unit,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ActionMenu(
        items = buildList {
            add(ActionMenuItem(R.drawable.ic_person, "멤버 관리", onMembersClick))
            add(ActionMenuItem(R.drawable.ic_map, "지도 관리", onManageClick))
            if (canLeave) add(ActionMenuItem(R.drawable.ic_exit, "나가기", onLeaveClick))
        },
        cornerRadius = MapDetailMenuCornerRadius,
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 300)
@Composable
private fun MapDetailMenuPreview() {
    MoaMapTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundSecondary),
        ) {
            MapDetailMenu(
                canLeave = true,
                onMembersClick = {},
                onManageClick = {},
                onLeaveClick = {},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = MapDetailTopBarHeight),
            )
        }
    }
}

/** 방장: 나갈 수 없어 나가기 줄이 없다. */
@Preview(showBackground = true, widthDp = 393, heightDp = 300)
@Composable
private fun MapDetailMenuOwnerPreview() {
    MoaMapTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundSecondary),
        ) {
            MapDetailMenu(
                canLeave = false,
                onMembersClick = {},
                onManageClick = {},
                onLeaveClick = {},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = MapDetailTopBarHeight),
            )
        }
    }
}
