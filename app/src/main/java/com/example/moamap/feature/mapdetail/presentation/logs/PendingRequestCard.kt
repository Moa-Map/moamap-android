package com.example.moamap.feature.mapdetail.presentation.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.component.ListCardShadowColor
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val CardShape = RoundedCornerShape(12.dp)
private val ActionButtonShape = RoundedCornerShape(8.dp)
private val ActionButtonHeight = 34.dp

/**
 * 장소 등록 요청 알림.
 *
 * 공개 지도의 방장·관리자에게만 보인다. 이 컴포저블은 그 판단을 하지 않는다 - 보여줄지 말지는
 * 목록을 넘기는 쪽에서 정한다.
 */
@Composable
internal fun PendingRequestCard(
    request: PendingRequestUiModel,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MoaMapPrimitiveColors.White,
        shadowColor = ListCardShadowColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LogAuthor(
                    userName = request.userName,
                    userImageUrl = request.userImageUrl,
                )
                LogTime(timeAgo = request.timeAgo)
            }

            Text(
                text = request.message,
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textNormal,
                // 아바타 폭만큼 들여써서 사용자명 아래로 문장이 이어지게 한다.
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RequestActionButton(
                    label = "수락",
                    background = MoaMapPrimitiveColors.Blue500,
                    onClick = onAcceptClick,
                )
                RequestActionButton(
                    label = "거절",
                    background = MoaMapPrimitiveColors.Gray200,
                    onClick = onRejectClick,
                )
            }
        }
    }
}

@Composable
private fun RowScope.RequestActionButton(
    label: String,
    background: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(ActionButtonHeight)
            .clip(ActionButtonShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MoaMapTheme.typography.button2,
            color = MoaMapTheme.colors.textWhite,
        )
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun PendingRequestCardPreview() {
    MoaMapTheme {
        PendingRequestCard(
            request = SamplePendingRequests.first(),
            onAcceptClick = {},
            onRejectClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
