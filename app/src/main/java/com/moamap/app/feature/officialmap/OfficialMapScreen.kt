package com.moamap.app.feature.officialmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.common.format.formatPlaceCount
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapGuideRow
import com.moamap.app.core.designsystem.component.MoaMapListPlaceholder
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.officialmap.domain.model.OfficialMap
import com.moamap.app.feature.officialmap.presentation.OfficialMapViewModel
import com.moamap.app.feature.officialmap.presentation.OfficialMapsState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** 안내 말풍선 바탕. 멤버 관리 역할 안내와 같이 진회색 70% 에 뒤 흐림(iOS `MoaMapTooltip` 기본). */
private val OfficialMapGuideScrim = MoaMapPrimitiveColors.Gray500.copy(alpha = 0.7f)

private const val OFFICIAL_MAP_GUIDE = "모아맵이 직접 만들어 제공하는 지도예요\n핫플부터 핵심정보 지도들을 한 번에 담아 가세요"

/** 카드 사진. 시안 「탐색탭_지도 · 공식지도 세부」(홈 커뮤니티 카드와 같은 틀). */
private val CardImageSize = 80.dp

/** 사진 오른쪽 글 칸. 사진(80) 높이에서 위아래 2 를 뺀 칸에 이름·설명은 위, 장소 수는 아래 오른쪽. */
private val CardTextAreaHeight = 76.dp

@Composable
fun OfficialMapScreen(
    onBackClick: () -> Unit = {},
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
        onRetryClick = viewModel::retry,
        onMapClick = onMapClick,
        modifier = modifier,
    )
}

@Composable
private fun OfficialMapContent(
    state: OfficialMapsState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onMapClick: (OfficialMap) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val guideHazeState = rememberHazeState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .hazeSource(guideHazeState)
            .background(MoaMapTheme.colors.backgroundPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        MoaMapTitleTopBar(title = "공식지도", onBackClick = onBackClick)

        // 시안 「공식지도」: 상단 바 아래 12 에 안내 줄, 안내 줄 ↔ 첫 카드·카드 사이 8.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MoaMapGuideRow(
                text = "공식지도가 궁금하신가요?",
                tooltipColor = OfficialMapGuideScrim,
                tooltipHazeState = guideHazeState,
            ) {
                Text(
                    text = OFFICIAL_MAP_GUIDE,
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapTheme.colors.textWhite,
                )
            }

            when (state) {
                OfficialMapsState.Loading -> MoaMapListPlaceholder {
                    CircularProgressIndicator()
                }

                is OfficialMapsState.Error -> MoaMapListPlaceholder {
                    MoaMapErrorNotice(message = state.message, onRetryClick = onRetryClick)
                }

                is OfficialMapsState.Success -> if (state.maps.isEmpty()) {
                    MoaMapListPlaceholder {
                        Text(
                            text = "아직 등록된 공식지도가 없어요",
                            style = MoaMapTheme.typography.body2,
                            color = MoaMapTheme.colors.textAssistive,
                        )
                    }
                } else {
                    state.maps.forEach { officialMap ->
                        OfficialMapCard(
                            map = officialMap,
                            onClick = { onMapClick(officialMap) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/**
 * 공식지도 카드. 시안 「탐색탭_지도」 지도 종류=공식지도 세부.
 *
 * 모서리 12, 그림자 0 0 8 4%, 여백 12, 사진 80 ↔ 글 12. 이름 + 인증 표시, 설명은 두 줄까지,
 * 오른쪽 아래에 장소 수. 장소 수는 서버 값 그대로 0 이어도 보인다(10-21 사용자 결정).
 * 참여는 카드가 아니라 지도 소개·지도 화면에서 한다.
 */
@Composable
private fun OfficialMapCard(
    map: OfficialMap,
    onClick: () -> Unit,
) {
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoThumbnail(imageUrl = map.imageUrl, size = CardImageSize)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(CardTextAreaHeight),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = map.title,
                            style = MoaMapTheme.typography.subtitle2.withDesignLineHeight(),
                            color = MoaMapTheme.colors.textNormal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_verify_filled),
                            contentDescription = "공식 인증",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    if (map.description.isNotEmpty()) {
                        Text(
                            text = map.description,
                            style = MoaMapTheme.typography.caption0.withDesignLineHeight(),
                            color = MoaMapTheme.colors.textNormal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location),
                        contentDescription = "등록 장소",
                        tint = MoaMapTheme.colors.textAssistive,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = formatPlaceCount(map.placeCount),
                        style = MoaMapTheme.typography.caption0,
                        color = MoaMapTheme.colors.textAssistive,
                        maxLines = 1,
                    )
                }
            }
        }
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
                        id = 17L,
                        title = "공중화장실 지도",
                        description = "행정안전부 공공데이터를 기반으로 전국 공중화장실의 위치와 개방 시간, 편의시설 정보를 보여주는 공식 지도입니다.",
                        imageUrl = null,
                        memberCount = 3,
                        placeCount = 0,
                        joined = false,
                    ),
                ),
            ),
            onBackClick = {},
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
            onRetryClick = {},
        )
    }
}
