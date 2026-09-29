package com.moamap.app.feature.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.BannerShadowBlurRadius
import com.moamap.app.core.designsystem.component.CardShadowColor
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.domain.model.CommunityMapSort
import com.moamap.app.feature.explore.presentation.BusinessInfoFooter
import com.moamap.app.feature.explore.presentation.CommunityMapCard
import com.moamap.app.feature.explore.presentation.CommunityMapSortRow
import com.moamap.app.feature.explore.presentation.CommunityMapsError
import com.moamap.app.feature.explore.presentation.CommunityMapsPlaceholder
import com.moamap.app.feature.explore.presentation.CommunityMapsState
import com.moamap.app.feature.explore.presentation.ExploreUiState
import com.moamap.app.feature.explore.presentation.ExploreViewModel
import com.moamap.app.feature.explore.presentation.RecommendedMapCard
import com.moamap.app.feature.mypage.ProfileMenu
import com.moamap.app.feature.mypage.rememberProfileMenuState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** 섹션 제목은 좌우 여백 안에서 4dp 더 들어간다. */
private val SectionTitlePadding = 4.dp

/** 가로 스크롤 목록이 화면 끝까지 흘러가도록, 여백을 콘텐츠 패딩으로 준다. */
private val HorizontalListPadding =
    PaddingValues(horizontal = MoaMapDimens.ScreenHorizontalPadding)

/** 이름을 아직 못 읽었을 때 추천 섹션 제목에 대신 쓰는 말. */
private const val DEFAULT_NICKNAME = "회원"

/**
 * 마지막 섹션과 사업자 정보 사이. 섹션 사이 간격과 같다.
 *
 * 시안 좌표로는 88 이지만, 접혀 있을 때도 그만큼 비어 보여 줄였다(09-28 사용자 요청).
 * 펼친 내용은 사업자 정보 아래로 늘어난다.
 */
private val FooterTopGap = 20.dp

/** 사업자 정보 안쪽 아래 여백. 시안 위 여백(25)과 같게 둔다. */
private val FooterBottomPadding = 25.dp

@Composable
fun ExploreScreen(
    /** 보던 자리 대신 맨 위에서 시작해야 하는지. 모음에서 로고로 들어온 경우다. */
    scrollToTop: Boolean = false,
    onScrolledToTop: () -> Unit = {},
    onProfileEditClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOfficialMapClick: () -> Unit = {},
    onCommunityMapClick: (CommunityMap) -> Unit = {},
    onSeeAllCommunityMapsClick: () -> Unit = {},
    /** 맨 아래 사업자 정보가 화면에 들어왔는지. 보이는 동안 하단 탭을 치우는 데 쓴다. */
    onFooterShownChange: (Boolean) -> Unit = {},
    /** 맨 위에 닿았다. 로고로 올라온 것처럼 손으로 끌지 않은 스크롤에도 하단 탭을 다시 꺼낸다. */
    onReachTop: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 지도에 참여하거나 나가고 돌아오면 카드의 참여 여부가 낡는다. 화면이 다시 보일 때 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    ExploreContent(
        uiState = uiState,
        scrollToTop = scrollToTop,
        onScrolledToTop = onScrolledToTop,
        onProfileEditClick = onProfileEditClick,
        onSettingsClick = onSettingsClick,
        onOfficialMapClick = onOfficialMapClick,
        onCommunityMapClick = onCommunityMapClick,
        onSeeAllCommunityMapsClick = onSeeAllCommunityMapsClick,
        onSortClick = viewModel::selectSort,
        onRetryClick = viewModel::retry,
        onFooterShownChange = onFooterShownChange,
        onReachTop = onReachTop,
        modifier = modifier,
    )
}

@Composable
private fun ExploreContent(
    uiState: ExploreUiState,
    onProfileEditClick: () -> Unit,
    scrollToTop: Boolean = false,
    onScrolledToTop: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onOfficialMapClick: () -> Unit,
    onCommunityMapClick: (CommunityMap) -> Unit,
    onSeeAllCommunityMapsClick: () -> Unit,
    onSortClick: (CommunityMapSort) -> Unit,
    onRetryClick: () -> Unit,
    onFooterShownChange: (Boolean) -> Unit = {},
    onReachTop: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val profileMenuState = rememberProfileMenuState()
    // 로고는 홈으로 돌아가는 버튼이다. 여기가 이미 홈이라 갈 곳이 없어 맨 위로 올린다.
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = profileMenuState.isVisible) {
        profileMenuState.dismiss()
    }

    // 사업자 정보는 스크롤 맨 끝에 있다. 남은 스크롤이 그 높이보다 작으면 화면에 들어온 것이다.
    // 맨 위에서는 세지 않는다 - 내용이 짧아 처음부터 보이면 하단 탭을 쓸 길이 없어진다.
    var footerHeightPx by remember { mutableIntStateOf(0) }
    val currentOnFooterShownChange by rememberUpdatedState(onFooterShownChange)
    val currentOnReachTop by rememberUpdatedState(onReachTop)
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value == 0 }
            .distinctUntilChanged()
            .filter { atTop -> atTop }
            .collect { currentOnReachTop() }
    }
    LaunchedEffect(scrollState) {
        snapshotFlow {
            footerHeightPx > 0 &&
                scrollState.value > 0 &&
                scrollState.maxValue - scrollState.value < footerHeightPx
        }
            .distinctUntilChanged()
            .collect { shown -> currentOnFooterShownChange(shown) }
    }

    // 로고로 들어온 경우. 신호를 받으면 맨 위로 올리고 바로 신호를 끈다.
    LaunchedEffect(scrollToTop) {
        if (!scrollToTop) return@LaunchedEffect
        scrollState.scrollTo(0)
        onScrolledToTop()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundPrimary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            ExploreTopBar(
                onLogoClick = { scope.launch { scrollState.animateScrollTo(0) } },
                onProfileClick = profileMenuState::show,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(top = 16.dp),
            ) {
                // 시안 「Home/」: 로고 줄 아래 16, 섹션 사이 20.
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    OfficialMapBanner(onClick = onOfficialMapClick)
                    // 읽지 못했거나 추천할 것이 없으면 제목까지 함께 감춘다.
                    if (uiState.recommendedMaps.isNotEmpty()) {
                        RecommendedMapSection(
                            nickname = uiState.nickname,
                            maps = uiState.recommendedMaps,
                            onMapClick = onCommunityMapClick,
                        )
                    }
                    CommunityMapSection(
                        uiState = uiState,
                        onSeeAllClick = onSeeAllCommunityMapsClick,
                        onSortClick = onSortClick,
                        onMapClick = onCommunityMapClick,
                        onRetryClick = onRetryClick,
                    )
                }

                Spacer(Modifier.height(FooterTopGap))
                // 사업자 정보가 보이면 하단 탭이 치워지므로 그 몫의 여백은 두지 않는다.
                // 시스템 내비게이션 바만큼만 더 내린다.
                BusinessInfoFooter(
                    bottomPadding = FooterBottomPadding +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    modifier = Modifier.onSizeChanged { size -> footerHeightPx = size.height },
                )
            }
        }

        if (profileMenuState.isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(profileMenuState.isVisible) {
                        detectTapGestures(onTap = { profileMenuState.dismiss() })
                    },
            )

            ProfileMenu(
                onProfileEditClick = {
                    profileMenuState.dismiss()
                    onProfileEditClick()
                },
                onSettingsClick = {
                    profileMenuState.dismiss()
                    onSettingsClick()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 5.dp, end = MoaMapDimens.ScreenHorizontalPadding),
            )
        }
    }
}

@Composable
private fun ExploreTopBar(
    onLogoClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding, vertical = 4.dp)
            .height(44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.img_moa_logo),
            contentDescription = "홈으로",
            modifier = Modifier
                .size(width = 74.dp, height = 44.dp)
                .clickable(role = Role.Button, onClick = onLogoClick),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // TODO: 알림 API 연동 후 복구
            // Icon(
            //     painter = painterResource(R.drawable.ic_notifications),
            //     contentDescription = "알림",
            //     tint = MoaMapPrimitiveColors.Black,
            //     modifier = Modifier
            //         .size(32.dp)
            //         .clickable {},
            // )
            // TODO: 알림 아이콘 복구 시 크기 32.dp / end 패딩 제거로 되돌린다.
            //  알림이 빠져 혼자 남은 동안만 키우고 안쪽으로 들인 값이다.
            Icon(
                painter = painterResource(R.drawable.ic_person),
                contentDescription = "프로필 메뉴",
                tint = MoaMapPrimitiveColors.Black,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(36.dp)
                    .clickable(onClick = onProfileClick),
            )
        }
    }
}

@Composable
private fun OfficialMapBanner(
    onClick: () -> Unit,
) {
    ShadowedSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
            .height(72.dp),
        shape = RoundedCornerShape(12.dp),
        color = MoaMapPrimitiveColors.Yellow100,
        shadowBlurRadius = BannerShadowBlurRadius,
        shadowColor = CardShadowColor,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_verify_filled),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "공공데이터 기반",
                    style = MoaMapTheme.typography.body1,
                    color = MoaMapPrimitiveColors.Yellow800,
                )
                Text(
                    text = "공식 지도 보러가기",
                    style = MoaMapTheme.typography.subtitle2,
                    color = MoaMapTheme.colors.textNormal,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_outward),
                contentDescription = null,
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MoaMapTheme.typography.title2,
        color = MoaMapTheme.colors.textNormal,
        modifier = Modifier.padding(
            horizontal = MoaMapDimens.ScreenHorizontalPadding + SectionTitlePadding,
        ),
    )
}

@Composable
private fun RecommendedMapSection(
    nickname: String,
    maps: List<CommunityMap>,
    onMapClick: (CommunityMap) -> Unit,
) {
    // 시안: 제목 ↔ 카드 12.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(text = "${nickname.ifBlank { DEFAULT_NICKNAME }}님을 위한 추천 지도")

        LazyRow(
            contentPadding = HorizontalListPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(maps, key = { it.id }) { map ->
                RecommendedMapCard(map = map, onClick = { onMapClick(map) })
            }
        }
    }
}

/**
 * 커뮤니티 지도 섹션. 시안 「Home/」: 제목 줄 ↔ 정렬 16, 정렬 ↔ 카드 12, 카드 사이 8.
 *
 * 고른 정렬로 앞 5개만 보여 준다. 칩과 나머지는 「전체보기」에서 본다.
 */
@Composable
private fun CommunityMapSection(
    uiState: ExploreUiState,
    onSeeAllClick: () -> Unit,
    onSortClick: (CommunityMapSort) -> Unit,
    onMapClick: (CommunityMap) -> Unit,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "커뮤니티 지도",
                style = MoaMapTheme.typography.title2.withDesignLineHeight(),
                color = MoaMapTheme.colors.textNormal,
            )
            // 글자와 화살표 모두 시안 #4A4F52(보조 글자색).
            Row(
                modifier = Modifier.clickable(role = Role.Button, onClick = onSeeAllClick),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "전체보기",
                    style = MoaMapTheme.typography.button2.withDesignLineHeight(),
                    color = MoaMapTheme.colors.textAlternative,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MoaMapTheme.colors.textAlternative,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CommunityMapSortRow(selected = uiState.sort, onClick = onSortClick)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (val state = uiState.communityMaps) {
                    CommunityMapsState.Loading -> CommunityMapsPlaceholder {
                        CircularProgressIndicator()
                    }

                    is CommunityMapsState.Error -> CommunityMapsPlaceholder {
                        CommunityMapsError(message = state.message, onRetryClick = onRetryClick)
                    }

                    is CommunityMapsState.Success -> {
                        if (state.maps.isEmpty()) {
                            CommunityMapsPlaceholder {
                                Text(
                                    text = "아직 등록된 지도가 없어요",
                                    style = MoaMapTheme.typography.body2,
                                    color = MoaMapTheme.colors.textAssistive,
                                )
                            }
                        } else {
                            state.maps.forEach { map ->
                                CommunityMapCard(map = map, onClick = { onMapClick(map) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun previewMaps(placeCount: Int) = List(3) { index ->
    CommunityMap(
        id = index + 1L,
        title = "서울 팝업스토어 맵",
        imageUrl = null,
        hashtags = listOf("맛집", "데이트코스", "데이트"),
        memberCount = 2312,
        placeCount = placeCount,
        joined = false,
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ExploreScreenPreview() {
    MoaMapTheme {
        ExploreContent(
            uiState = ExploreUiState(
                communityMaps = CommunityMapsState.Success(previewMaps(placeCount = 116)),
                nickname = "모아맵",
                // 추천 카드는 장소 수를 그리지 않아 서버도 주지 않는다.
                recommendedMaps = previewMaps(placeCount = 0),
            ),
            onProfileEditClick = {},
            onSettingsClick = {},
            onOfficialMapClick = {},
            onCommunityMapClick = {},
            onSeeAllCommunityMapsClick = {},
            onSortClick = {},
            onRetryClick = {},
        )
    }
}
