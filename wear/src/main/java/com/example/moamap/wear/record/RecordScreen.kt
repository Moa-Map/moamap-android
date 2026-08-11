package com.example.moamap.wear.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.moamap.wear.theme.MoaWearColors
import java.util.Locale

@Composable
fun RecordScreen(viewModel: RecordViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pointState by viewModel.pointState.collectAsStateWithLifecycle()

    // 원형 화면은 세로 여유가 빠듯하다. 작은 워치에서 아래 버튼이 잘리지 않게 스크롤을 연다.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (val state = uiState) {
            is RecordUiState.Idle -> IdleContent(
                onStart = viewModel::start,
                enabled = !pointState.isBusy,
            )

            is RecordUiState.Recording -> RecordingContent(
                state = state,
                onStop = viewModel::stop,
                enabled = !pointState.isBusy,
            )

            is RecordUiState.Finished -> FinishedContent(
                state = state,
                onRetry = viewModel::retry,
                onReset = viewModel::reset,
            )

            is RecordUiState.PermissionDenied -> PermissionDeniedContent(
                state = state,
                onReset = viewModel::reset,
            )
        }

        // 위치 보내기는 기록 상태와 무관하다. 다만 전송 결과 화면과 권한 거부 화면에서는
        // 사용자가 먼저 해결해야 할 일이 있어 내보내지 않는다.
        if (uiState is RecordUiState.Idle || uiState is RecordUiState.Recording) {
            SendPointSlot(state = pointState, onSend = viewModel::sendCurrentLocation)
        }
    }
}

@Composable
private fun IdleContent(onStart: () -> Unit, enabled: Boolean) {
    Text(
        text = "모아 발자취",
        style = MaterialTheme.typography.labelMedium,
        color = MoaWearColors.Gray200,
    )
    Button(
        onClick = onStart,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "외출 기록 시작")
    }
}

@Composable
private fun RecordingContent(
    state: RecordUiState.Recording,
    onStop: () -> Unit,
    enabled: Boolean,
) {
    Text(
        text = formatElapsed(state.elapsedMillis),
        style = MaterialTheme.typography.numeralMedium,
    )
    Text(
        text = state.latestHeartRate?.let { "♥ ${it.toInt()} bpm" } ?: "♥ 측정 중",
        style = MaterialTheme.typography.bodyMedium,
        color = MoaWearColors.Yellow500,
    )
    Button(
        onClick = onStop,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "기록 종료")
    }
}

/**
 * 위치 보내기 자리. 상태가 바뀌어도 같은 자리를 차지한다.
 *
 * 자리가 밀리면 눌러야 할 버튼이 움직여서, 성공 안내가 뜨는 순간 옆 버튼을 잘못 누르게 된다.
 */
@Composable
private fun SendPointSlot(state: PointSendState, onSend: () -> Unit) {
    when (state) {
        PointSendState.Idle -> SendPointButton(text = "📍 현재 위치 보내기", onClick = onSend)

        PointSendState.Sending -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text(text = "보내는 중…", style = MaterialTheme.typography.bodySmall)
        }

        PointSendState.Sent -> Text(
            text = "✓ 보냈어요",
            style = MaterialTheme.typography.bodyMedium,
            color = MoaWearColors.Blue200,
        )

        is PointSendState.Failed -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MoaWearColors.StatusAlert,
            )
            SendPointButton(text = "다시 보내기", onClick = onSend)
        }
    }
}

/** 주 버튼보다 한 단계 낮은 위계. 둘이 같은 무게면 어느 쪽이 주 동작인지 읽히지 않는다. */
@Composable
private fun SendPointButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MoaWearColors.Blue900,
            contentColor = MoaWearColors.Blue200,
        ),
    ) {
        Text(text = text)
    }
}

@Composable
private fun FinishedContent(
    state: RecordUiState.Finished,
    onRetry: () -> Unit,
    onReset: () -> Unit,
) {
    Text(text = "샘플 ${state.sampleCount}개", style = MaterialTheme.typography.bodyMedium)
    when (state.transferState) {
        TransferState.SENDING -> Text(text = "폰으로 보내는 중…")
        TransferState.SUCCESS -> {
            Text(text = "전송 완료")
            Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text(text = "새 기록") }
        }
        // 전송 실패 화면에는 대기 상태로 나가는 버튼을 두지 않는다.
        // 못 보낸 세션이 남아 있는 동안에는 새 기록을 시작할 수 없기 때문이다.
        TransferState.FAILED -> {
            Text(
                text = "전송 실패. 기록은 워치에 있어요",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(text = "다시 보내기")
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(state: RecordUiState.PermissionDenied, onReset: () -> Unit) {
    Text(
        text = state.message,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
    )
    Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text(text = "다시 시도") }
}

private fun formatElapsed(elapsedMillis: Long): String {
    val totalSeconds = (elapsedMillis / 1_000).coerceAtLeast(0)
    return String.format(
        Locale.KOREA,
        "%02d:%02d:%02d",
        totalSeconds / 3600,
        (totalSeconds % 3600) / 60,
        totalSeconds % 60,
    )
}
