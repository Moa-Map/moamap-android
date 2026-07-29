package com.example.moamap.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * 지도 상세 상단바.
 *
 * 우측은 아이콘이 아니라 텍스트 하나다. 참여 여부와 역할에 따라 참여하기·나가기가
 * 오가고, 서버가 거절할 게 뻔한 경우에는 비활성으로 남는다 - `MapDetail.topBarAction` 참고.
 */
@Composable
internal fun MapDetailTopBar(
    mapTitle: String,
    roleBadge: String?,
    action: MapDetailAction,
    onBackClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
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
                .padding(horizontal = 72.dp),
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

        MapDetailTopBarAction(
            action = action,
            enabled = actionEnabled,
            onClick = onActionClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp),
        )
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
        modifier = modifier.clickable(enabled = clickable, onClick = onClick),
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
        )
    }
}
