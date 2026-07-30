package com.example.moamap.feature.footprint.watchrecord

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
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
    onRecommendSingleClick: () -> Unit,
    onRecommendMultiClick: () -> Unit,
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
                                    onRecommendSingleClick = onRecommendSingleClick,
                                    onRecommendMultiClick = onRecommendMultiClick,
                                )

                                SessionKind.SINGLE_POINT -> SinglePointCard(
                                    session = session,
                                    onFindPlaceClick = onFindPlaceClick,
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
    onRecommendSingleClick: () -> Unit,
    onRecommendMultiClick: () -> Unit,
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

            RecommendSplitButton(
                onLeftClick = onRecommendSingleClick,
                onRightClick = onRecommendMultiClick,
            )
        }
    }
}

/**
 * 워치에서 한 번 눌러 보낸 좌표 하나.
 *
 * 산책 카드와 달리 갈래가 하나뿐이라 좌우로 나뉜 버튼을 쓰지 않는다.
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
 * 임시. 겉보기엔 버튼 하나지만 누른 쪽에 따라 다른 추천 목록을 띄운다.
 *
 * 추천 API 가 붙기 전까지 두 가지 결과를 화면에서 바로 견줘보려고 둔 것이다. 왼쪽 절반은
 * 한 곳만, 오른쪽 절반은 미리 지정한 여러 곳을 준다.
 */
@Composable
private fun RecommendSplitButton(
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RecommendButtonHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(MoaMapTheme.colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "추천 장소 확인하기",
            style = MoaMapTheme.typography.subtitle4,
            color = MoaMapPrimitiveColors.White,
        )

        // 글자 위에 얹어 어느 쪽을 눌러도 두 영역 중 하나가 반드시 받게 한다.
        Row(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .noRippleClickable(onClick = onLeftClick),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .noRippleClickable(onClick = onRightClick),
            )
        }
    }
}

/**
 * 물결 없이 클릭만 받는다.
 *
 * 반쪽짜리 영역에 물결이 번지면 버튼이 둘로 갈라져 보인다. 겉보기에는 버튼 하나여야 한다.
 */
@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
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
