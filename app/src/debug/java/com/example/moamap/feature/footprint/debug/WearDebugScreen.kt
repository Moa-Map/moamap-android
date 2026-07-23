package com.example.moamap.feature.footprint.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moamap.feature.footprint.domain.model.ReceivedWalkSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WearDebugScreen(
    onShare: (ReceivedWalkSession) -> Unit,
    viewModel: WearDebugViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "수신된 워치 세션", style = MaterialTheme.typography.titleLarge)
        Button(onClick = viewModel::refresh, modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = "새로고침")
        }

        when (val state = uiState) {
            is WearDebugUiState.Loading -> CircularProgressIndicator()
            is WearDebugUiState.Success -> if (state.sessions.isEmpty()) {
                Text(text = "아직 받은 세션이 없어요. 워치에서 기록을 종료해 보세요.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.sessions) { session ->
                        SessionCard(session = session, onShare = { onShare(session) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: ReceivedWalkSession, onShare: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = formatTime(session.receivedAtEpochMillis),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = "세션 ID: ${session.payload.clientSessionId}")
            Text(text = "기록 시간: ${formatDuration(session.stats.durationMillis)}")
            Text(
                text = "샘플 ${session.stats.sampleCount}개 " +
                    "(위치 ${session.stats.locationSampleCount} / 심박 ${session.stats.heartRateSampleCount})",
            )
            Text(text = "심박 커버리지: ${(session.stats.heartRateCoverageRatio * 100).roundToInt()}%")
            Button(onClick = onShare) { Text(text = "JSON 공유") }
        }
    }
}

private fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("MM월 dd일 HH:mm", Locale.KOREA).format(Date(epochMillis))

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1_000
    if (totalSeconds < 60) {
        return "${totalSeconds}초"
    }
    val totalMinutes = totalSeconds / 60
    return "${totalMinutes / 60}시간 ${totalMinutes % 60}분"
}
