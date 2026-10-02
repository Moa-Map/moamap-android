package com.moamap.app.feature.mapdetail.presentation.intro

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ButtonShadowColor
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaMapBackButton
import com.moamap.app.core.designsystem.component.MoaMapTopBarIconEdgePadding
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.domain.model.MapPlacePreview
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import com.moamap.app.feature.mapdetail.presentation.MapLoadState
import com.moamap.app.feature.mapdetail.presentation.mapOrNull

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
        onMorePlacesClick = viewModel::showMorePlaces,
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
    onMorePlacesClick: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: (@Composable (List<MapPlace>) -> Unit)? = null,
) {
    // 공식지도는 히어로 대신 상단 바를 둔다(시안 「공식지도 - 상세」). 종류는 지도를 받아야 안다.
    val official = uiState.map.mapOrNull?.type == MapType.Official

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
                Column(
                    modifier = if (official) Modifier.statusBarsPadding() else Modifier,
                ) {
                    if (official) {
                        OfficialIntroTopBar(title = state.map.title, onBackClick = onBackClick)
                    }
                    MapIntroBody(
                        map = state.map,
                        places = uiState.places,
                        mapPlaces = uiState.mapPlaces,
                        onPreviewClick = onPreviewClick,
                        // 커뮤니티는 전체 목록 화면이 없어 미리보기로 보낸다. 공식지도는 장소가
                        // 많아 그 자리에서 펼친다.
                        onMoreClick = if (official) onMorePlacesClick else onPreviewClick,
                        mapContent = mapContent,
                        modifier = Modifier.weight(1f),
                    )
                }
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

        // 공식지도는 상단 바가 뒤로가기를 갖는다.
        if (!official) {
            // 히어로 위에 겹치는 뒤로가기. 상단바를 따로 두지 않는다 - 피그마에도 제목 없이
            // 아이콘만 있고, 히어로가 화면 맨 위까지 올라와야 그라데이션이 살아난다.
            MoaMapBackButton(
                onClick = onBackClick,
                // 히어로 위에서는 흰색이라야 읽힌다. 아직 히어로가 없는 로딩·오류 화면은
                // 밝은 배경뿐이라 같은 색을 쓰면 아이콘이 보이지 않는다.
                tint = if (uiState.map is MapLoadState.Success) {
                    MoaMapTheme.colors.textWhite
                } else {
                    MoaMapTheme.colors.textNormal
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = MoaMapTopBarIconEdgePadding),
            )
        }

        ErrorSnackbar(
            message = uiState.errorMessage,
            onShown = onErrorShown,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * 소개 본문.
 *
 * 공식지도는 히어로(제작자)와 태그가 없고, 간격을 공식 상세 시안(`3278:24025`)에 맞춘다 -
 * 상단 바 아래 20, 지도 제목 아래 12, 장소 카드 사이 8, `더보기` 위 16.
 */
@Composable
private fun MapIntroBody(
    map: MapDetail,
    places: MapPlacePreview,
    mapPlaces: List<MapPlace>,
    onPreviewClick: () -> Unit,
    onMoreClick: () -> Unit,
    mapContent: (@Composable (List<MapPlace>) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val official = map.type == MapType.Official
    val tags = if (official) emptyList() else map.tags

    /**
     * 지도를 만지는 동안인지.
     *
     * 지도는 세로 스크롤 안에 들어 있어, 잠그지 않으면 지도를 끌 때 화면이 같이 움직인다.
     * 손이 지도에 닿아 있는 동안만 스크롤을 멈춘다.
     */
    var mapTouched by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = !mapTouched),
    ) {
        if (!official) {
            MapIntroHero(
                title = map.title,
                ownerName = map.ownerName,
                imageUrl = map.imageUrl,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (official) 20.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 소개할 게 아무것도 없으면 제목만 남은 빈 섹션이 된다. 통째로 건너뛴다.
            if (tags.isNotEmpty() || map.description != null) {
                IntroSection {
                    MapIntroSectionTitle("지도 소개")
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (tags.isNotEmpty()) {
                            MapIntroTagRow(tags = tags)
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
                Spacer(Modifier.height(if (official) 12.dp else 8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MapIntroMapHeight)
                        .clip(RoundedCornerShape(8.dp))
                        // 손이 닿는 동안만 바깥 스크롤을 멈춘다. 이벤트를 가로채지는 않아
                        // 지도는 그대로 받는다.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    mapTouched = event.changes.any { change -> change.pressed }
                                }
                            }
                        },
                ) {
                    if (mapContent != null) {
                        mapContent(mapPlaces)
                    } else {
                        MapIntroMap(
                            places = mapPlaces,
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
                    Column(verticalArrangement = Arrangement.spacedBy(if (official) 8.dp else 4.dp)) {
                        places.places.forEach { place ->
                            MapIntroPlaceItem(
                                name = place.name,
                                address = place.address,
                                photoUrl = place.photoUrl,
                            )
                        }
                    }
                    if (places.hasMore) {
                        Spacer(Modifier.height(if (official) 16.dp else 8.dp))
                        MapIntroMoreLink(
                            onClick = onMoreClick,
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

/** 공식지도 상세 상단 바. 시안 GNB: 높이 58, 왼쪽 20 에 뒤로가기 32, 가운데 지도 이름. */
@Composable
private fun OfficialIntroTopBar(
    title: String,
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
    ) {
        MoaMapBackButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = MoaMapTopBarIconEdgePadding),
        )
        // 긴 이름이 뒤로가기 밑으로 파고들지 않게 좌우를 같이 밀어 가운데를 지킨다.
        Text(
            text = title,
            style = MoaMapTheme.typography.title3,
            color = MoaMapTheme.colors.textNormal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 72.dp),
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
    personal = false,
    inviteCode = null,
)

/** `더보기` 가 뜨도록 처음 보여줄 개수보다 많게 둔다. */
private val PreviewPlaces = List(INTRO_PLACE_COUNT + 2) { index ->
    MapPlace(
        id = index + 1L,
        name = "커피나무 ${index + 1}호점",
        address = "서울 성동구 성수이로 ${index + 1}",
        latitude = 37.5445 + index * 0.001,
        longitude = 127.0557 + index * 0.001,
        photoUrl = null,
    )
}

@Composable
private fun MapIntroPreviewContent(map: MapDetail) {
    MoaMapTheme {
        MapIntroContent(
            uiState = MapIntroUiState(
                map = MapLoadState.Success(map),
                mapPlaces = PreviewPlaces,
            ),
            onBackClick = {},
            onPreviewClick = {},
            onRetryClick = {},
            onJoinClick = {},
            onMorePlacesClick = {},
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

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapIntroScreenPreview() {
    MapIntroPreviewContent(PreviewMap)
}

@Preview(showBackground = true, widthDp = 393, heightDp = 1162)
@Composable
private fun OfficialMapIntroScreenPreview() {
    MapIntroPreviewContent(
        PreviewMap.copy(
            title = "서울 무장애 여행지",
            description = "휠체어로 다니기 좋은 서울의 관광지를 모았어요.",
            ownerName = null,
            type = MapType.Official,
            tags = emptyList(),
        ),
    )
}
