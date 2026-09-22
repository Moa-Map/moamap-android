package com.moamap.app.feature.mapdetail.presentation.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.MapDetailTopBarHeight
import com.moamap.app.feature.mapdetail.presentation.logs.MapLogUiModel
import com.moamap.app.feature.mapdetail.presentation.logs.MapLogsContent
import com.moamap.app.feature.mapdetail.presentation.logs.PendingRequestUiModel
import com.moamap.app.feature.mapdetail.presentation.logs.SampleMapLogs
import com.moamap.app.feature.mapdetail.presentation.logs.SamplePendingRequests

/**
 * 지도 관리. 상단바 메뉴에서 들어온다.
 *
 * 장소 등록 요청 알림과 멤버의 장소 추가·삭제 기록을 모아 본다. 알림은 수락·거절할 수 있는
 * 사람에게만 넘어온다 - 프라이빗 지도는 누구나 장소를 바로 추가해 기록만 남는다.
 *
 * 지도 상세 위에 겹쳐 그린다. 요청을 수락하면 뒤의 지도가 새 장소를 다시 읽어야 하는데, 같은
 * 화면 안에 있어야 그 신호를 따로 넘기지 않고 받는다.
 */
@Composable
internal fun MapManageScreen(
    pendingRequests: List<PendingRequestUiModel>,
    logs: List<MapLogUiModel>,
    loading: Boolean,
    errorMessage: String?,
    requestActionEnabled: Boolean,
    onAcceptClick: (Long) -> Unit,
    onRejectClick: (Long) -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            // 뒤에 깔린 지도로 터치가 새지 않게 빈 자리의 탭을 여기서 받는다.
            .pointerInput(Unit) { detectTapGestures() }
            .statusBarsPadding(),
    ) {
        MapManageTopBar(onBackClick = onBackClick)
        MapLogsContent(
            pendingRequests = pendingRequests,
            logs = logs,
            loading = loading,
            errorMessage = errorMessage,
            requestActionEnabled = requestActionEnabled,
            onAcceptClick = onAcceptClick,
            onRejectClick = onRejectClick,
            onRetryClick = onRetryClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MapManageTopBar(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MapDetailTopBarHeight),
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
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = "지도 관리",
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapManageScreenPreview() {
    MoaMapTheme {
        MapManageScreen(
            pendingRequests = SamplePendingRequests,
            logs = SampleMapLogs,
            loading = false,
            errorMessage = null,
            requestActionEnabled = true,
            onAcceptClick = {},
            onRejectClick = {},
            onRetryClick = {},
            onBackClick = {},
        )
    }
}

/** 프라이빗 지도: 수락할 사람이 없어 기록만 있다. */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapManageScreenPrivatePreview() {
    MoaMapTheme {
        MapManageScreen(
            pendingRequests = emptyList(),
            logs = SampleMapLogs,
            loading = false,
            errorMessage = null,
            requestActionEnabled = true,
            onAcceptClick = {},
            onRejectClick = {},
            onRetryClick = {},
            onBackClick = {},
        )
    }
}
