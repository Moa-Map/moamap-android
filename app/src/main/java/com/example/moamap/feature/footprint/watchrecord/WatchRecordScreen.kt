package com.example.moamap.feature.footprint.watchrecord

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.core.walksession.SessionKind
import com.example.moamap.feature.footprint.domain.model.ReceivedWalkSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val RecommendButtonHeight = 48.dp

/** 워치에서 받아 저장해 둔 걷기 세션 목록. */
@Composable
internal fun WatchRecordScreen(
    onBackClick: () -> Unit,
    /** @param multiplePlaces 추천 장소를 여러 곳 띄울지. [recommendsMultiplePlaces] 가 정한다. */
    onRecommendClick: (multiplePlaces: Boolean) -> Unit,
    onFindPlaceClick: (lat: Double, lng: Double) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WatchRecordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                            when (session.payload.kind) {
                                SessionKind.WALK -> WalkSessionCard(
                                    session = session,
                                    // 지우고 나서 넘어간다. 순서가 뒤바뀌면 화면이 떠난 뒤에
                                    // 지워져, 돌아왔을 때 카드가 남아 있는지가 타이밍에 걸린다.
                                    onRecommendClick = {
                                        viewModel.deleteSession(session)
                                        onRecommendClick(session.recommendsMultiplePlaces())
                                    },
                                )

                                SessionKind.SINGLE_POINT -> SinglePointCard(
                                    session = session,
                                    onFindPlaceClick = { lat, lng ->
                                        viewModel.deleteSession(session)
                                        onFindPlaceClick(lat, lng)
                                    },
                                )
                            }
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
private fun WalkSessionCard(
    session: ReceivedWalkSession,
    onRecommendClick: () -> Unit,
) {
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

            Spacer(Modifier.height(4.dp))

            RecommendButton(onClick = onRecommendClick)
        }
    }
}

/**
 * 워치에서 한 번 눌러 보낸 좌표 하나.
 *
 * 산책 카드와 마찬가지로 버튼을 누르면 이 기록은 폰에서 지워진다. 확인은 묻지 않는다.
 */
@Composable
private fun SinglePointCard(
    session: ReceivedWalkSession,
    onFindPlaceClick: (lat: Double, lng: Double) -> Unit,
) {
    // 좌표가 없는 단일 좌표 세션은 워치가 보내지 않는다. 그래도 들어오면 보여줄 것이 없다.
    val point = session.payload.samples.firstOrNull { it.lat != null && it.lng != null } ?: return
    val lat = point.lat ?: return
    val lng = point.lng ?: return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "📍 ${formatTime(session.receivedAtEpochMillis)}",
                style = MoaMapTheme.typography.title3,
            )
            Text(
                text = formatCoordinate(lat, lng),
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textAlternative,
            )

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RecommendButtonHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MoaMapTheme.colors.primary)
                    .clickable { onFindPlaceClick(lat, lng) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "이 위치 장소 추가하기",
                    style = MoaMapTheme.typography.subtitle4,
                    color = MoaMapPrimitiveColors.White,
                )
            }
        }
    }
}

/** 다섯 자리면 1m 남짓이다. 그보다 길게 보여줘도 읽는 사람에게 의미가 없다. */
internal fun formatCoordinate(lat: Double, lng: Double): String =
    String.format(Locale.US, "%.5f, %.5f", lat, lng)

/**
 * 추천 장소로 넘어가는 버튼.
 *
 * 누르면 이 기록은 폰에서 지워진다. 확인을 묻지 않는다.
 *
 * 전에는 겉보기만 버튼 하나이고 왼쪽 절반과 오른쪽 절반이 각각 한 곳·여러 곳을 띄우는
 * 임시 장치였다. 두 결과를 눈으로 견주려던 것이라 규칙이라 할 게 없었고, 지금은 기록 시간이
 * 정한다 - [recommendsMultiplePlaces] 참고.
 */
@Composable
private fun RecommendButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RecommendButtonHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(MoaMapTheme.colors.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "추천 장소 확인하기",
            style = MoaMapTheme.typography.subtitle4,
            color = MoaMapPrimitiveColors.White,
        )
    }
}

/**
 * 내보낸 JSON 파일을 다른 앱으로 넘긴다. 임시 파일이라 읽기 권한을 함께 준다.
 *
 * 카드 버튼이 추천 흐름으로 바뀌면서 부르는 곳이 없어졌다. 워치가 보낸 원본을 눈으로
 * 확인할 일이 남아 있어 [WatchRecordViewModel.exportForShare] 와 함께 지우지 않고 둔다.
 */
@Suppress("unused")
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
