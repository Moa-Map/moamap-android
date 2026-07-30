package com.example.moamap.feature.mapdetail.presentation.logs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.collection.domain.model.MapType

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
 * 항목 간격을 `verticalArrangement` 로 주지 않는다. 타임라인 항목 사이가 벌어지면 레일의
 * 세로선이 끊긴다. 간격은 각 항목이 아래쪽 여백으로 직접 들고 있고, 선은 그 여백까지 덮는다.
 */
@Composable
internal fun MapLogsContent(
    pendingRequests: List<PendingRequestUiModel>,
    logs: List<MapLogUiModel>,
    onAcceptClick: (Long) -> Unit,
    onRejectClick: (Long) -> Unit,
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

        if (logs.isEmpty()) {
            item(key = "activity-empty") { EmptyLogs() }
        } else {
            itemsIndexed(logs, key = { _, log -> log.id }) { index, log ->
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
private fun EmptyLogs() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "아직 활동 내역이 없어요",
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapLogsContentPublicPreview() {
    MoaMapTheme {
        MapLogsContent(
            pendingRequests = SamplePendingRequests,
            logs = SampleMapLogs,
            onAcceptClick = {},
            onRejectClick = {},
        )
    }
}

/** 프라이빗 지도: 알림 카드가 없고 권한 변경 로그도 없다. */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapLogsContentPrivatePreview() {
    MoaMapTheme {
        MapLogsContent(
            pendingRequests = emptyList(),
            logs = SampleMapLogs.forMapType(MapType.Private),
            onAcceptClick = {},
            onRejectClick = {},
        )
    }
}
