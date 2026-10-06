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
import com.moamap.app.core.designsystem.component.ActionMenuItem
import com.moamap.app.core.designsystem.component.ModalActionMenu
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import dev.chrisbanes.haze.HazeState

/** 시안 「모달창」 바탕색: #4A4F52 의 60%. */
private val MapDetailMenuScrim = MoaMapPrimitiveColors.Gray500.copy(alpha = 0.6f)

/** 메뉴 위 끝. 상단 바 가운데에 놓인 메뉴 아이콘(32)의 아래 끝에 붙는다(시안). */
internal val MapDetailMenuTop = (MapDetailTopBarHeight + 32.dp) / 2

/** 메뉴 오른쪽 끝에서 화면 끝까지. 시안은 아이콘(오른쪽 20)보다 조금 안쪽이다. */
internal val MapDetailMenuEnd = 25.dp

/**
 * 상단바 메뉴. 참여한 지도에서 우측 위 아이콘을 누르면 뜬다.
 *
 * 뒤를 흐리지 않고 회색만 깐다. 메뉴 뒤 대부분이 지도인데 지도는 Compose 흐림이 잡지 못한다 -
 * [ModalActionMenu] 참고.
 *
 * @param canShareInviteCode 초대코드 줄을 넣을지. 초대코드를 쥔 프라이빗 지도 참여자다 -
 * `MapDetail.shareableInviteCode` 참고.
 * @param canLeave 나가기 줄을 넣을지. 나갈 수 없는 사람에게는 줄 자체를 보여주지 않는다 -
 * `MapDetail.canLeaveFromMenu` 참고.
 */
@Composable
internal fun MapDetailMenu(
    canShareInviteCode: Boolean,
    canLeave: Boolean,
    onMembersClick: () -> Unit,
    onManageClick: () -> Unit,
    onInviteCodeClick: () -> Unit,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 바탕으로 흐릴 화면. 지도가 TextureView 라 지도까지 흐려진다 - `MapDetailMap` 참고. */
    hazeState: HazeState? = null,
) {
    ModalActionMenu(
        items = buildList {
            add(ActionMenuItem(R.drawable.ic_person, "멤버 관리", onMembersClick))
            add(ActionMenuItem(R.drawable.ic_map, "지도 관리", onManageClick))
            if (canShareInviteCode) add(ActionMenuItem(R.drawable.ic_key, "초대코드", onInviteCodeClick))
            if (canLeave) add(ActionMenuItem(R.drawable.ic_exit, "나가기", onLeaveClick))
        },
        scrimColor = MapDetailMenuScrim,
        hazeState = hazeState,
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
                canShareInviteCode = false,
                canLeave = true,
                onMembersClick = {},
                onManageClick = {},
                onInviteCodeClick = {},
                onLeaveClick = {},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -MapDetailMenuEnd, y = MapDetailMenuTop),
            )
        }
    }
}

/** 프라이빗 지도: 초대코드가 메뉴에 들어 있고 나가기는 없다(모음 탭 편집에서 나간다). */
@Preview(showBackground = true, widthDp = 393, heightDp = 300)
@Composable
private fun MapDetailMenuPrivatePreview() {
    MoaMapTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundSecondary),
        ) {
            MapDetailMenu(
                canShareInviteCode = true,
                canLeave = false,
                onMembersClick = {},
                onManageClick = {},
                onInviteCodeClick = {},
                onLeaveClick = {},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -MapDetailMenuEnd, y = MapDetailMenuTop),
            )
        }
    }
}
