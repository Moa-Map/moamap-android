package com.example.moamap.feature.mapdetail.presentation.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapTheme

/** 탭바가 위에 겹쳐 있어 그만큼 목록을 내려서 시작한다. */
private val TabBarClearance = 90.dp

/** 시안의 섹션 간 간격. */
private val SectionGap = 20.dp

/**
 * 로그 탭 내용.
 *
 * **지도 타입·역할 분기를 여기서 하지 않는다.** 알림을 보여줄지는 [pendingRequests] 를 넘기는
 * 쪽이 정한다. 그래야 프리뷰로 공개·프라이빗 두 경우를 모두 만들 수 있다.
 *
 * 활동 내역 자리만 [loading]·[errorMessage] 를 탄다. 장소 등록 요청은 목록 위에 얹히는
 * 곁가지라 자기 자리에 로딩과 오류를 그리지 않는다 - 못 읽으면 카드가 없고, 안내는 스낵바가
 * 맡는다. 여기에 상태를 하나 더 두면 활동 내역이 다 떴는데도 화면이 계속 기다리는 것처럼 보인다.
 *
 * 항목 간격을 `verticalArrangement` 로 주지 않는다. 타임라인 항목 사이가 벌어지면 레일의
 * 세로선이 끊긴다. 간격은 각 항목이 아래쪽 여백으로 직접 들고 있고, 선은 그 여백까지 덮는다.
 */
@Composable
internal fun MapLogsContent(
    pendingRequests: List<PendingRequestUiModel>,
    logs: List<MapLogUiModel>,
    loading: Boolean,
    errorMessage: String?,
    /** 수락·거절이 오가는 중에는 모든 카드의 버튼을 잠근다. 한 번에 하나만 처리한다. */
    requestActionEnabled: Boolean,
    onAcceptClick: (Long) -> Unit,
    onRejectClick: (Long) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MoaMapDimens.ScreenHorizontalPadding,
            end = MoaMapDimens.ScreenHorizontalPadding,
            top = TabBarClearance,
            bottom = 32.dp,
        ),
    ) {
        items(pendingRequests, key = { request -> "request-${request.id}" }) { request ->
            PendingRequestCard(
                request = request,
                actionEnabled = requestActionEnabled,
                onAcceptClick = { onAcceptClick(request.id) },
                onRejectClick = { onRejectClick(request.id) },
                modifier = Modifier.padding(bottom = SectionGap),
            )
        }

        item(key = "activity-header") {
            Text(
                text = "활동 내역",
                style = MoaMapTheme.typography.subtitle1,
                color = MoaMapTheme.colors.textNormal,
                modifier = Modifier.padding(bottom = SectionGap),
            )
        }

        when {
            loading -> item(key = "activity-loading") { LoadingLogs() }

            errorMessage != null -> item(key = "activity-error") {
                LogsError(message = errorMessage, onRetryClick = onRetryClick)
            }

            logs.isEmpty() -> item(key = "activity-empty") { EmptyLogs() }

            else -> itemsIndexed(logs, key = { _, log -> log.id }) { index, log ->
                MapLogItem(
                    log = log,
                    hasLineAbove = index > 0,
                    hasLineBelow = index < logs.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun LoadingLogs() {
    CenteredLogsNotice {
        CircularProgressIndicator(
            color = MoaMapTheme.colors.textAssistive,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun EmptyLogs() {
    CenteredLogsNotice {
        Text(
            text = "아직 활동 내역이 없어요",
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
        )
    }
}

@Composable
private fun LogsError(message: String, onRetryClick: () -> Unit) {
    CenteredLogsNotice {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = message,
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textAssistive,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onRetryClick) {
                Text(
                    text = "다시 시도",
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textNormal,
                )
            }
        }
    }
}

/** 목록 자리를 대신 채우는 안내. 세 상태가 같은 높이를 써야 탭을 오갈 때 덜컹이지 않는다. */
@Composable
private fun CenteredLogsNotice(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapLogsContentPreview() {
    MoaMapTheme {
        MapLogsContent(
            pendingRequests = SamplePendingRequests,
            logs = SampleMapLogs,
            loading = false,
            errorMessage = null,
            requestActionEnabled = true,
            onAcceptClick = {},
            onRejectClick = {},
            onRetryClick = {},
        )
    }
}

/** 프라이빗 지도: 알림 카드가 없다. 후기 작성 로그는 반대로 여기에만 나온다. */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapLogsContentPrivatePreview() {
    MoaMapTheme {
        MapLogsContent(
            pendingRequests = emptyList(),
            logs = SampleMapLogs,
            loading = false,
            errorMessage = null,
            requestActionEnabled = true,
            onAcceptClick = {},
            onRejectClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapLogsContentErrorPreview() {
    MoaMapTheme {
        MapLogsContent(
            pendingRequests = emptyList(),
            logs = emptyList(),
            loading = false,
            errorMessage = ACTIVITY_LOAD_FAILED_MESSAGE,
            requestActionEnabled = true,
            onAcceptClick = {},
            onRejectClick = {},
            onRetryClick = {},
        )
    }
}
