package com.example.moamap.feature.footprint.watchrecord

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.footprint.domain.model.ReceivedWalkSession
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** 워치에서 받아 저장해 둔 걷기 세션 목록. */
@Composable
internal fun WatchRecordScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WatchRecordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        WatchRecordTopBar(onBackClick = onBackClick)

        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = viewModel::refresh,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(text = "새로고침")
            }

            when (val state = uiState) {
                is WatchRecordUiState.Loading -> CircularProgressIndicator()
                is WatchRecordUiState.Success -> if (state.sessions.isEmpty()) {
                    Text(text = "아직 받은 세션이 없어요. 워치에서 기록을 종료해 보세요.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.sessions) { session ->
                            SessionCard(
                                session = session,
                                onShare = {
                                    scope.launch {
                                        context.shareSessionJson(viewModel.exportForShare(session))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchRecordTopBar(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MoaMapPrimitiveColors.White),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
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
            text = "워치 기록",
            style = MoaMapTheme.typography.title3,
            color = MoaMapPrimitiveColors.Black,
            modifier = Modifier.align(Alignment.Center),
        )
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
                style = MoaMapTheme.typography.title3,
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

/** 내보낸 JSON 파일을 다른 앱으로 넘긴다. 임시 파일이라 읽기 권한을 함께 준다. */
private fun Context.shareSessionJson(file: File) {
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "세션 JSON 공유"))
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
