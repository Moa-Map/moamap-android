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

/** 시안 「모달창」 바탕색: #4A4F52 의 70%. */
private val MapDetailMenuScrim = MoaMapPrimitiveColors.Gray500.copy(alpha = 0.7f)

/** 메뉴 위 끝. 상단 바 가운데에 놓인 메뉴 아이콘(32)의 아래 끝에 붙는다(시안). */
internal val MapDetailMenuTop = (MapDetailTopBarHeight + 32.dp) / 2

/** 메뉴 오른쪽 끝에서 화면 끝까지. 시안은 메뉴 아이콘의 오른쪽 끝(20)에 맞춘다. */
internal val MapDetailMenuEnd = 20.dp

/**
 * 상단바 메뉴. 참여한 지도에서 우측 위 아이콘을 누르면 뜬다.
 *
 * 뒤에 깔린 지도까지 흐린다. 지도가 TextureView 라 Compose 흐림이 잡는다 - `MapDetailMap` 참고.
 * 메뉴 밖 화면은 어둡게 덮지 않는다(시안).
 *
 * @param showsMembers 멤버 관리 줄을 넣을지. 나만의 지도는 뺀다 - `MapDetail.showsMembersMenu` 참고.
 * @param canEditInfo 지도 정보 줄을 넣을지. 지도를 만든 방장만이다 - `MapDetail.canEditInfo` 참고.
 * @param canShareInviteCode 초대코드 줄을 넣을지. 초대코드를 쥔 프라이빗 지도 참여자다 -
 * `MapDetail.shareableInviteCode` 참고.
 * @param canLeave 나가기 줄을 넣을지. 커뮤니티 지도 참여자다. 방장도 넣고 누르면 안내만 띄운다 -
 * `MapDetail.canLeaveFromMenu` 참고.
 */
@Composable
internal fun MapDetailMenu(
    showsMembers: Boolean,
    canEditInfo: Boolean,
    canShareInviteCode: Boolean,
    canLeave: Boolean,
    onMembersClick: () -> Unit,
    onManageClick: () -> Unit,
    onInfoClick: () -> Unit,
    onInviteCodeClick: () -> Unit,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 바탕으로 흐릴 화면. 지도가 TextureView 라 지도까지 흐려진다 - `MapDetailMap` 참고. */
    hazeState: HazeState? = null,
) {
    ModalActionMenu(
        items = buildList {
            if (showsMembers) add(ActionMenuItem(R.drawable.ic_person, "멤버 관리", onMembersClick))
            add(ActionMenuItem(R.drawable.ic_map, "장소 관리", onManageClick))
            if (canEditInfo) add(ActionMenuItem(R.drawable.ic_info, "지도 정보", onInfoClick))
            if (canShareInviteCode) add(ActionMenuItem(R.drawable.ic_key, "초대코드", onInviteCodeClick))
            if (canLeave) add(ActionMenuItem(R.drawable.ic_exit, "나가기", onLeaveClick, danger = true))
        },
        scrimColor = MapDetailMenuScrim,
        hazeState = hazeState,
        modifier = modifier,
    )
}

/**
 * 장소 페이지 상단바 메뉴. 고치거나 지울 수 있는 장소에서 우측 위 아이콘을 누르면 뜬다 -
 * `MapDetail.canManagePlace` 참고. 시안 릴리즈 v1 「장소선택 - 내가 등록한 장소」. 자리·모양은 지도 상세
 * 메뉴와 같다.
 */
@Composable
internal fun PlaceDetailMenu(
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    ModalActionMenu(
        items = listOf(
            ActionMenuItem(R.drawable.ic_edit, "수정하기", onEditClick),
            ActionMenuItem(R.drawable.ic_trash, "삭제하기", onDeleteClick),
        ),
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
                showsMembers = true,
                canEditInfo = false,
                canShareInviteCode = false,
                canLeave = true,
                onMembersClick = {},
                onManageClick = {},
                onInfoClick = {},
                onInviteCodeClick = {},
                onLeaveClick = {},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -MapDetailMenuEnd, y = MapDetailMenuTop),
            )
        }
    }
}

/** 프라이빗 지도 방장: 지도 정보·초대코드가 메뉴에 들어 있고 나가기는 없다(모음 탭 편집에서 나간다). */
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
                showsMembers = true,
                canEditInfo = true,
                canShareInviteCode = true,
                canLeave = false,
                onMembersClick = {},
                onManageClick = {},
                onInfoClick = {},
                onInviteCodeClick = {},
                onLeaveClick = {},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -MapDetailMenuEnd, y = MapDetailMenuTop),
            )
        }
    }
}
