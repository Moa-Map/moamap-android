package com.example.moamap.feature.mapdetail.presentation.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.ButtonShadowBlurRadius
import com.example.moamap.core.designsystem.component.ButtonShadowColor
import com.example.moamap.core.designsystem.component.ErrorSnackbar
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.mapdetail.domain.model.MapDetail
import com.example.moamap.feature.mapdetail.domain.model.MapPlace
import com.example.moamap.feature.mapdetail.domain.model.MapPlacePreview
import com.example.moamap.feature.mapdetail.domain.model.MapRole
import com.example.moamap.feature.mapdetail.presentation.MapLoadState

/** 본문 좌우 여백. 피그마의 폭 354dp 를 393dp 화면에서 뺀 값. */
private val ContentHorizontalPadding = 20.dp

/** 하단 고정 버튼에 마지막 섹션이 가리지 않도록 확보하는 높이. */
private val BottomButtonReservedHeight = 86.dp

@Composable
fun MapIntroScreen(
    onBackClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onJoined: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapIntroViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 미리보기로 상세에 들어가 거기서 참여하고 돌아오면 하단 버튼이 낡는다. 다시 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    LaunchedEffect(uiState.joined) {
        if (uiState.joined) onJoined()
    }

    MapIntroContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onPreviewClick = onPreviewClick,
        onRetryClick = viewModel::retry,
        onJoinClick = viewModel::join,
        onErrorShown = viewModel::consumeErrorMessage,
        modifier = modifier,
    )
}

@Composable
internal fun MapIntroContent(
    uiState: MapIntroUiState,
    onBackClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onRetryClick: () -> Unit,
    onJoinClick: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: (@Composable (List<MapPlace>) -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary),
    ) {
        when (val state = uiState.map) {
            MapLoadState.Loading -> LoadingContent()

            is MapLoadState.Error -> ErrorContent(
                message = state.message,
                onRetryClick = onRetryClick,
            )

            is MapLoadState.Success -> {
                MapIntroBody(
                    map = state.map,
                    places = uiState.places,
                    onPreviewClick = onPreviewClick,
                    mapContent = mapContent,
                )
                // 이미 참여한 지도라면 버튼이 할 일이 없다. 상세로 갈 길은 미리보기가 있다.
                if (!state.map.joined) {
                    JoinButton(
                        enabled = !uiState.joining,
                        onClick = onJoinClick,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(
                                start = ContentHorizontalPadding,
                                end = ContentHorizontalPadding,
                                bottom = 16.dp,
                            ),
                    )
                }
            }
        }

        BackButton(
            onClick = onBackClick,
            // 히어로 위에서는 흰색이라야 읽힌다. 아직 히어로가 없는 로딩·오류 화면은
            // 밝은 배경뿐이라 같은 색을 쓰면 아이콘이 보이지 않는다.
            onHero = uiState.map is MapLoadState.Success,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp),
        )

        ErrorSnackbar(
            message = uiState.errorMessage,
            onShown = onErrorShown,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun MapIntroBody(
    map: MapDetail,
    places: MapPlacePreview,
    onPreviewClick: () -> Unit,
    mapContent: (@Composable (List<MapPlace>) -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        MapIntroHero(
            title = map.title,
            ownerName = map.ownerName,
            imageUrl = map.imageUrl,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 소개할 게 아무것도 없으면 제목만 남은 빈 섹션이 된다. 통째로 건너뛴다.
            if (map.tags.isNotEmpty() || map.description != null) {
                IntroSection {
                    MapIntroSectionTitle("지도 소개")
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (map.tags.isNotEmpty()) {
                            MapIntroTagRow(tags = map.tags)
                        }
                        if (map.description != null) {
                            Text(
                                text = map.description,
                                style = MoaMapTheme.typography.body3,
                                color = MoaMapTheme.colors.textAlternative,
                            )
                        }
                    }
                }
                IntroSection { MapIntroDivider() }
            }

            IntroSection {
                MapIntroSectionTitle("지도")
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MapIntroMapHeight)
                        .clip(RoundedCornerShape(8.dp)),
                ) {
                    if (mapContent != null) {
                        mapContent(places.places)
                    } else {
                        MapIntroMap(
                            places = places.places,
                            onClick = onPreviewClick,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    PreviewButton(
                        onClick = onPreviewClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                    )
                }
            }

            if (places.places.isNotEmpty()) {
                IntroSection { MapIntroDivider() }
                IntroSection {
                    MapIntroSectionTitle("장소 목록")
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        places.places.forEach { place ->
                            MapIntroPlaceItem(place = place)
                        }
                    }
                    if (places.hasMore) {
                        Spacer(Modifier.height(8.dp))
                        MapIntroMoreLink(
                            onClick = onPreviewClick,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }

            Spacer(Modifier.height(BottomButtonReservedHeight))
        }
    }
}

/** 본문 섹션 하나. 좌우 여백을 한곳에서 준다. */
@Composable
private fun IntroSection(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentHorizontalPadding),
        content = content,
    )
}

/**
 * 히어로 위에 겹치는 뒤로가기.
 *
 * 상단바를 따로 두지 않는다. 피그마에도 제목 없이 아이콘만 있고, 히어로가 화면 맨 위까지
 * 올라와야 그라데이션이 살아난다.
 */
@Composable
private fun BackButton(
    onClick: () -> Unit,
    onHero: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_left),
            contentDescription = "뒤로가기",
            tint = if (onHero) MoaMapTheme.colors.textWhite else MoaMapTheme.colors.textNormal,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PreviewButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ShadowedSurface(
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = MoaMapPrimitiveColors.Blue500,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ButtonShadowColor,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "미리보기",
                style = MoaMapTheme.typography.button2,
                color = MoaMapTheme.colors.textWhite,
            )
        }
    }
}

@Composable
private fun JoinButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) MoaMapPrimitiveColors.Blue500 else MoaMapPrimitiveColors.Gray100,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ButtonShadowColor,
        onClick = { if (enabled) onClick() },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "참여하기",
                style = MoaMapTheme.typography.button0,
                color = MoaMapTheme.colors.textWhite,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
}

private val PreviewMap = MapDetail(
    id = 1L,
    title = "성수 카페 투어",
    description = "성수동에서 하루를 보내기 좋은 카페들을 모았어요.\n주말 오후에 가기 좋습니다.",
    imageUrl = null,
    ownerName = "모아",
    type = MapType.Community,
    role = MapRole.None,
    tags = listOf("카페", "데이트", "성수"),
    memberCount = 128,
    placeCount = 12,
    joined = false,
)

private val PreviewPlaces = MapPlacePreview(
    places = List(4) { index ->
        MapPlace(
            id = index + 1L,
            name = "커피나무 ${index + 1}호점",
            address = "서울 성동구 성수이로 ${index + 1}",
            latitude = 37.5445 + index * 0.001,
            longitude = 127.0557 + index * 0.001,
            photoUrl = null,
        )
    },
    hasMore = true,
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapIntroScreenPreview() {
    MoaMapTheme {
        MapIntroContent(
            uiState = MapIntroUiState(
                map = MapLoadState.Success(PreviewMap),
                places = PreviewPlaces,
            ),
            onBackClick = {},
            onPreviewClick = {},
            onRetryClick = {},
            onJoinClick = {},
            onErrorShown = {},
            mapContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MoaMapPrimitiveColors.Blue50),
                )
            },
        )
    }
}
