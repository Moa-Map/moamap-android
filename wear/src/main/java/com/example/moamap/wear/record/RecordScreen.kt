package com.example.moamap.wear.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import java.util.Locale

@Composable
fun RecordScreen(viewModel: RecordViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = uiState) {
            is RecordUiState.Idle -> IdleContent(onStart = viewModel::start)
            is RecordUiState.Recording -> RecordingContent(state = state, onStop = viewModel::stop)
            is RecordUiState.Finished -> FinishedContent(state = state, onRetry = viewModel::retry)
            is RecordUiState.PermissionDenied -> Text(
                text = state.message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun IdleContent(onStart: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "모아 발자취", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onStart) { Text(text = "외출 기록 시작") }
    }
}

@Composable
private fun RecordingContent(state: RecordUiState.Recording, onStop: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = formatElapsed(state.elapsedMillis), style = MaterialTheme.typography.titleLarge)
        Text(
            text = state.latestHeartRate?.let { "♥ ${it.toInt()} bpm" } ?: "♥ 측정 중",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(text = "샘플 ${state.sampleCount}", style = MaterialTheme.typography.bodySmall)
        Button(onClick = onStop) { Text(text = "기록 종료") }
    }
}

@Composable
private fun FinishedContent(state: RecordUiState.Finished, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "샘플 ${state.sampleCount}개", style = MaterialTheme.typography.bodyMedium)
        when (state.transferState) {
            TransferState.SENDING -> Text(text = "폰으로 보내는 중…")
            TransferState.SUCCESS -> Text(text = "전송 완료")
            TransferState.FAILED -> {
                Text(
                    text = "전송 실패. 기록은 워치에 있어요",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = onRetry) { Text(text = "다시 보내기") }
            }
        }
    }
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
