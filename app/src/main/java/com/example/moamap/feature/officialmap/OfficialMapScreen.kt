package com.example.moamap.feature.officialmap

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.MapCard
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.core.common.format.formatMemberCount
import com.example.moamap.core.common.format.formatPlaceCount
import com.example.moamap.feature.officialmap.domain.model.OfficialMap
import com.example.moamap.feature.officialmap.presentation.OfficialMapViewModel
import com.example.moamap.feature.officialmap.presentation.OfficialMapsState

@Composable
fun OfficialMapScreen(
    onBackClick: () -> Unit = {},
    onDensityMapClick: () -> Unit = {},
    onMapClick: (OfficialMap) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: OfficialMapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 지도에 참여하거나 나가고 돌아오면 카드가 낡는다. 화면이 다시 보일 때 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    OfficialMapContent(
        state = state,
        onBackClick = onBackClick,
        onDensityMapClick = onDensityMapClick,
        onRetryClick = viewModel::retry,
        onMapClick = onMapClick,
        onJoinClick = { officialMap -> viewModel.join(officialMap.id) },
        modifier = modifier,
    )
}

@Composable
private fun OfficialMapContent(
    state: OfficialMapsState,
    onBackClick: () -> Unit,
    onDensityMapClick: () -> Unit,
    onRetryClick: () -> Unit,
    onMapClick: (OfficialMap) -> Unit = {},
    onJoinClick: (OfficialMap) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        OfficialMapTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // 목록 조회와 무관하게 항상 그린다. 서버가 죽어도 밀집도 화면으로는 들어갈 수 있어야 한다.
            DensityMapEntryCard(onClick = onDensityMapClick)

            when (state) {
                OfficialMapsState.Loading -> OfficialMapsPlaceholder {
                    CircularProgressIndicator()
                }

                is OfficialMapsState.Error -> OfficialMapsPlaceholder {
                    ErrorContent(message = state.message, onRetryClick = onRetryClick)
                }

                is OfficialMapsState.Success -> if (state.maps.isEmpty()) {
                    OfficialMapsPlaceholder {
                        Text(
                            text = "아직 등록된 공식지도가 없어요",
                            style = MoaMapTheme.typography.body2,
                            color = MoaMapTheme.colors.textAssistive,
                        )
                    }
                } else {
                    state.maps.forEach { officialMap ->
                        MapCard(
                            title = officialMap.title,
                            description = officialMap.description,
                            memberCount = formatMemberCount(officialMap.memberCount),
                            placeCount = formatPlaceCount(officialMap.placeCount),
                            joined = officialMap.joined,
                            verified = true,
                            descriptionStyle = MoaMapTheme.typography.caption0,
                            onClick = { onMapClick(officialMap) },
                            onJoinClick = { onJoinClick(officialMap) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/**
 * 실시간 유동인구 지도 진입점.
 *
 * 서버 공식지도 목록에 없는 카드다. 밀집도는 지도 엔티티가 아니라 공공데이터 조회
 * API 라 `GET /api/v1/maps/official` 이 내려주지 않는다. 메타 숫자는 시안 값이다.
 */
@Composable
private fun DensityMapEntryCard(
    onClick: () -> Unit,
) {
    MapCard(
        title = "실시간 유동인구 지도",
        description = "서울 주요 명소의 실시간 인구 밀집도를 한눈에",
        memberCount = "2.3천명",
        placeCount = "116곳",
        joined = false,
        verified = true,
        descriptionStyle = MoaMapTheme.typography.caption0,
        onClick = onClick,
        onJoinClick = {},
    )
}

/** 목록 자리에 로딩·오류를 같은 높이로 앉혀 화면이 튀지 않게 한다. */
@Composable
private fun OfficialMapsPlaceholder(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textAssistive,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "다시 시도",
            style = MoaMapTheme.typography.button2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.clickable(onClick = onRetryClick),
        )
    }
}

@Composable
private fun OfficialMapTopBar(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
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
            text = "공식지도",
            style = MoaMapTheme.typography.title3,
            color = MoaMapPrimitiveColors.Black,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun OfficialMapScreenPreview() {
    MoaMapTheme {
        OfficialMapContent(
            state = OfficialMapsState.Success(
                listOf(
                    OfficialMap(
                        id = 6L,
                        title = "화장실 위치",
                        description = "공공데이터 기반 공중화장실 위치",
                        memberCount = 1,
                        placeCount = 5416,
                        joined = false,
                    ),
                ),
            ),
            onBackClick = {},
            onDensityMapClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun OfficialMapScreenErrorPreview() {
    MoaMapTheme {
        OfficialMapContent(
            state = OfficialMapsState.Error("공식지도를 불러오지 못했어요"),
            onBackClick = {},
            onDensityMapClick = {},
            onRetryClick = {},
        )
    }
}
