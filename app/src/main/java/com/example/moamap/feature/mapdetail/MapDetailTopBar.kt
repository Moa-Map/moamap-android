package com.example.moamap.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.mapdetail.domain.model.MapDetailAction

private val RoleBadgeShape = RoundedCornerShape(999.dp)

/**
 * 제목을 좌우에서 밀어 두는 여백.
 *
 * 제목은 화면 한가운데 놓여야 해서 좌우를 같은 값으로 잡는다. 우측에 글자가 하나 더
 * 붙으면(초대코드) 그만큼 넓혀 준다 - 그러지 않으면 긴 제목이 액션 글자 밑으로 파고든다.
 */
private val TitleSidePadding = 72.dp
private val TitleSidePaddingWithInviteCode = 132.dp

/**
 * 우측 액션 글자 위아래로 넓히는 터치 영역.
 *
 * 글자 높이가 18dp 뿐이라 그대로 두면 누를 자리가 너무 얇다. `clickable` **뒤에** 두어야
 * 여백이 클릭 영역 안으로 들어간다 - 앞에 두면 여백만큼 밀리고 누르는 자리는 그대로다.
 *
 * 가로는 건드리지 않는다. 폭을 48dp 로 고정하면 51dp 인 "초대코드" 가 말줄임으로 잘린다.
 * 옆 버튼과는 12dp 를 띄워 두어 잘못 눌릴 일이 없다.
 */
private val ActionTouchPadding = 12.dp

/**
 * 지도 상세 상단바.
 *
 * 우측은 아이콘이 아니라 텍스트다. 참여 여부와 역할에 따라 참여하기·나가기가 오가고,
 * 서버가 거절할 게 뻔한 경우에는 비활성으로 남는다 - `MapDetail.topBarAction` 참고.
 *
 * 프라이빗 지도에 참여한 사람에게는 그 왼쪽에 초대코드가 하나 더 붙는다. 나가기를 밀어내지
 * 않고 왼쪽에 세우는 건, 되돌릴 수 없는 쪽을 늘 같은 자리에 두기 위해서다.
 */
@Composable
internal fun MapDetailTopBar(
    mapTitle: String,
    roleBadge: String?,
    action: MapDetailAction,
    onBackClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 초대코드 버튼에 실을 코드. null 이면 버튼을 띄우지 않는다. */
    inviteCode: String? = null,
    onInviteCodeClick: () -> Unit = {},
    /** 요청이 도는 동안 잠근다. 라벨은 그대로 두고 누를 수만 없게 한다. */
    actionEnabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(MoaMapTheme.colors.backgroundSecondary),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .size(48.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "뒤로가기",
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier.size(32.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                // 좌우 아이콘·액션과 겹치지 않도록 안쪽으로 밀어 둔다.
                .padding(
                    horizontal = if (inviteCode != null) {
                        TitleSidePaddingWithInviteCode
                    } else {
                        TitleSidePadding
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = mapTitle,
                style = MoaMapTheme.typography.title3,
                color = MoaMapTheme.colors.textNormal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (roleBadge != null) {
                Text(
                    text = roleBadge,
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapPrimitiveColors.Blue900,
                    // 배경을 먼저 깐다. 순서를 뒤집으면 나중에 그려지는 배경이 테두리를 덮는다.
                    modifier = Modifier
                        .background(
                            color = MoaMapPrimitiveColors.Blue50,
                            shape = RoleBadgeShape,
                        )
                        .border(
                            width = 1.dp,
                            color = MoaMapPrimitiveColors.Blue500,
                            shape = RoleBadgeShape,
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (inviteCode != null) {
                Text(
                    text = "초대코드",
                    style = MoaMapTheme.typography.button2,
                    color = MoaMapPrimitiveColors.Blue600,
                    maxLines = 1,
                    modifier = Modifier
                        .clickable(onClick = onInviteCodeClick)
                        .padding(vertical = ActionTouchPadding),
                )
            }

            MapDetailTopBarAction(
                action = action,
                enabled = actionEnabled,
                onClick = onActionClick,
            )
        }
    }
}

@Composable
private fun MapDetailTopBarAction(
    action: MapDetailAction,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (action) {
        MapDetailAction.Join -> "참여하기"
        MapDetailAction.Leave, MapDetailAction.LeaveDisabled -> "나가기"
        MapDetailAction.None -> return
    }

    // 비활성은 두 갈래다. 서버가 거절할 경우(LeaveDisabled)와 요청이 도는 중(!enabled).
    val clickable = enabled && action != MapDetailAction.LeaveDisabled
    val color = when {
        !clickable -> MoaMapTheme.colors.textDisable
        action == MapDetailAction.Join -> MoaMapPrimitiveColors.Blue600
        else -> MoaMapTheme.colors.statusAlert
    }

    Text(
        text = label,
        style = MoaMapTheme.typography.button2,
        color = color,
        maxLines = 1,
        modifier = modifier
            .clickable(enabled = clickable, onClick = onClick)
            .padding(vertical = ActionTouchPadding),
    )
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun MapDetailTopBarJoinPreview() {
    MoaMapTheme {
        MapDetailTopBar(
            mapTitle = "서울 데이트 지도",
            roleBadge = null,
            action = MapDetailAction.Join,
            onBackClick = {},
            onActionClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun MapDetailTopBarLeavePreview() {
    MoaMapTheme {
        MapDetailTopBar(
            mapTitle = "서울 데이트 지도",
            roleBadge = "방장",
            action = MapDetailAction.Leave,
            onBackClick = {},
            onActionClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun MapDetailTopBarPrivatePreview() {
    MoaMapTheme {
        MapDetailTopBar(
            mapTitle = "우리끼리 맛집",
            roleBadge = null,
            action = MapDetailAction.LeaveDisabled,
            onBackClick = {},
            onActionClick = {},
            inviteCode = "A1B2C3",
        )
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun MapDetailTopBarPrivateLongTitlePreview() {
    MoaMapTheme {
        MapDetailTopBar(
            mapTitle = "우리끼리만 아는 성수동 맛집 모음",
            roleBadge = null,
            action = MapDetailAction.Leave,
            onBackClick = {},
            onActionClick = {},
            inviteCode = "A1B2C3",
        )
    }
}
