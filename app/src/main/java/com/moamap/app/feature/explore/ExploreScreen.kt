package com.moamap.app.feature.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapListPlaceholder
import com.moamap.app.core.designsystem.component.MoaMapTabTopBarHeight
import com.moamap.app.core.designsystem.component.MoaMapTopBarLogo
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.explore.domain.model.CommunityMap
import com.moamap.app.feature.explore.presentation.CommunityMapCard
import com.moamap.app.feature.explore.presentation.CommunityMapsState
import com.moamap.app.feature.explore.presentation.ExploreUiState
import com.moamap.app.feature.explore.presentation.ExploreViewModel
import com.moamap.app.feature.explore.presentation.HomeBannerCarousel
import com.moamap.app.feature.mypage.ProfileMenu
import com.moamap.app.feature.officialmap.domain.model.OfficialMap
import com.moamap.app.feature.officialmap.presentation.OfficialMapsState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

/**
 * 홈 배경. 시안 「메인 화면」이 색 변수(`Background/Primary`, #E6F6FF)가 아니라 이 값을 직접 칠했다.
 * 다른 화면은 그대로 변수 색을 쓴다(10-03 사용자 결정).
 */
private val HomeBackground = Color(0xFFE7F4FB)

/** 가로 스크롤 목록이 화면 끝까지 흘러가도록, 여백을 콘텐츠 패딩으로 준다. */
private val HorizontalListPadding =
    PaddingValues(horizontal = MoaMapDimens.ScreenHorizontalPadding)

/**
 * 맨 아래 내용이 하단 탭에 가리지 않게 남기는 높이. 하단 탭은 늘 떠 있다.
 *
 * 하단 탭 아래 띄움 12 + 하단 탭 58. 시안의 마지막 카드 ↔ 하단 탭 32 는 섹션 사이 간격([SectionGap])이
 * 이 칸 앞에도 들어가 채운다. 시스템 내비게이션 바는 따로 더한다.
 */
private val BottomBarClearance = 70.dp

/**
 * 상단 바 오른쪽 끝 여백. 프로필 아이콘(24)을 누르기 쉽게 40 칸에 담아서, 그림이 화면 끝에서
 * 시안대로 24 에 오도록 칸 여백(8)을 뺀다.
 */
private val TopBarEndPadding = 16.dp
private val TopBarIconSize = 24.dp
private val ProfileIconTouchSize = 40.dp

/**
 * 알림 아이콘(24)을 누르는 칸. 시안의 알림 ↔ 프로필 아이콘 사이 12 를 알림 칸 여백(4)과 프로필 칸 여백(8)이
 * 채운다 - 둘 사이에 따로 띄우지 않는다.
 */
private val NotificationIconTouchSize = 32.dp

/**
 * 프로필 메뉴 위치. 이번 시안에 메뉴가 없어 예전처럼 프로필 아이콘 바로 아래(4)·오른쪽 끝을 맞춘다.
 * 아이콘은 58 상단 바 가운데라 위 17 에서 41 까지다.
 */
private val ProfileMenuTop = 45.dp
private val ProfileMenuEnd = 24.dp

/** 시안 「메인 화면」: 상단 바 아래 12 에 히어로, 히어로·커뮤니티·공식 지도 사이 32. */
private val ContentTopGap = 12.dp
private val SectionGap = 32.dp

/** 홈 커뮤니티 카드. 시안 「탐색탭_지도」는 전체보기 카드보다 사진이 작고 글과 가깝다. */
private val CommunityCardImageSize = 80.dp
private val CommunityCardImageGap = 12.dp

/** 공식 지도 카드 사진. 이름 폭도 이 폭에 맞춘다. */
private val OfficialCardImageSize = 110.dp

@Composable
fun ExploreScreen(
    /** 보던 자리 대신 맨 위에서 시작해야 하는지. 모음에서 로고로 들어온 경우다. */
    scrollToTop: Boolean = false,
    onScrolledToTop: () -> Unit = {},
    onProfileEditClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onCommunityMapClick: (CommunityMap) -> Unit = {},
    onSeeAllCommunityMapsClick: () -> Unit = {},
    onOfficialMapClick: (OfficialMap) -> Unit = {},
    onSeeAllOfficialMapsClick: () -> Unit = {},
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
        onNotificationClick = onNotificationClick,
        onCommunityMapClick = onCommunityMapClick,
        onSeeAllCommunityMapsClick = onSeeAllCommunityMapsClick,
        onCommunityRetryClick = viewModel::retryCommunityMaps,
        onOfficialMapClick = onOfficialMapClick,
        onSeeAllOfficialMapsClick = onSeeAllOfficialMapsClick,
        onOfficialRetryClick = viewModel::retryOfficialMaps,
        modifier = modifier,
    )
}

@Composable
private fun ExploreContent(
    uiState: ExploreUiState,
    onProfileEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onCommunityMapClick: (CommunityMap) -> Unit,
    onSeeAllCommunityMapsClick: () -> Unit,
    onCommunityRetryClick: () -> Unit,
    onOfficialMapClick: (OfficialMap) -> Unit,
    onSeeAllOfficialMapsClick: () -> Unit,
    onOfficialRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollToTop: Boolean = false,
    onScrolledToTop: () -> Unit = {},
) {
    var profileMenuVisible by remember { mutableStateOf(false) }
    // 프로필 메뉴가 홈 화면을 흐려 바탕으로 쓴다. 메뉴는 이 원본 밖(위)에 그린다.
    val hazeState = rememberHazeState()
    // 로고는 홈으로 돌아가는 버튼이다. 여기가 이미 홈이라 갈 곳이 없어 맨 위로 올린다.
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = profileMenuVisible) {
        profileMenuVisible = false
    }

    // 로고로 들어온 경우. 신호를 받으면 맨 위로 올리고 바로 신호를 끈다.
    LaunchedEffect(scrollToTop) {
        if (!scrollToTop) return@LaunchedEffect
        scrollState.scrollTo(0)
        onScrolledToTop()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(HomeBackground)
                .statusBarsPadding(),
        ) {
            ExploreTopBar(
                onLogoClick = { scope.launch { scrollState.animateScrollTo(0) } },
                onNotificationClick = onNotificationClick,
                onProfileClick = { profileMenuVisible = true },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(top = ContentTopGap),
                verticalArrangement = Arrangement.spacedBy(SectionGap),
            ) {
                HomeBannerCarousel(banners = uiState.banners)
                CommunityMapSection(
                    state = uiState.communityMaps,
                    onSeeAllClick = onSeeAllCommunityMapsClick,
                    onMapClick = onCommunityMapClick,
                    onRetryClick = onCommunityRetryClick,
                )
                OfficialMapSection(
                    state = uiState.officialMaps,
                    onSeeAllClick = onSeeAllOfficialMapsClick,
                    onMapClick = onOfficialMapClick,
                    onRetryClick = onOfficialRetryClick,
                )

                Spacer(
                    Modifier
                        .navigationBarsPadding()
                        .height(BottomBarClearance),
                )
            }
        }

        if (profileMenuVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(profileMenuVisible) {
                        detectTapGestures(onTap = { profileMenuVisible = false })
                    },
            )

            ProfileMenu(
                hazeState = hazeState,
                onProfileEditClick = {
                    profileMenuVisible = false
                    onProfileEditClick()
                },
                onSettingsClick = {
                    profileMenuVisible = false
                    onSettingsClick()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = ProfileMenuTop, end = ProfileMenuEnd),
            )
        }
    }
}

/**
 * 홈 상단 바. 시안 「탐색탭_앱바」: 높이 58, 바탕 `Background/Primary`(홈 바탕과 조금 다름), 왼쪽 로고,
 * 오른쪽 끝 24 에 채운 알림·프로필 아이콘 24 가 사이 12 로 놓인다.
 */
@Composable
private fun ExploreTopBar(
    onLogoClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MoaMapTheme.colors.backgroundPrimary)
            .height(MoaMapTabTopBarHeight)
            .padding(start = MoaMapDimens.ScreenHorizontalPadding, end = TopBarEndPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoaMapTopBarLogo(onClick = onLogoClick)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(NotificationIconTouchSize)
                .clickable(role = Role.Button, onClick = onNotificationClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bell_filled),
                contentDescription = "알림",
                tint = MoaMapPrimitiveColors.Gray800,
                modifier = Modifier.size(TopBarIconSize),
            )
        }
        Box(
            modifier = Modifier
                .size(ProfileIconTouchSize)
                .clickable(role = Role.Button, onClick = onProfileClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_user_filled),
                contentDescription = "프로필 메뉴",
                tint = MoaMapPrimitiveColors.Gray800,
                modifier = Modifier.size(TopBarIconSize),
            )
        }
    }
}

/** 섹션 제목 줄: 제목(왼쪽 2 안쪽) + 「전체보기 >」. 글자와 화살표 모두 시안 #4A4F52(보조 글자색). */
@Composable
private fun SectionHeader(
    title: String,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MoaMapTheme.typography.title2.withDesignLineHeight(),
            color = MoaMapTheme.colors.textNormal,
        )
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
}

/**
 * 커뮤니티 지도 섹션. 시안: 제목 줄 ↔ 카드 16, 카드 사이 8.
 *
 * 인기순 앞 3개만 보여 준다. 정렬·칩과 나머지는 「전체보기」에서 본다.
 */
@Composable
private fun CommunityMapSection(
    state: CommunityMapsState,
    onSeeAllClick: () -> Unit,
    onMapClick: (CommunityMap) -> Unit,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(title = "커뮤니티 지도", onSeeAllClick = onSeeAllClick)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state) {
                CommunityMapsState.Loading -> MoaMapListPlaceholder {
                    CircularProgressIndicator()
                }

                is CommunityMapsState.Error -> MoaMapListPlaceholder {
                    MoaMapErrorNotice(message = state.message, onRetryClick = onRetryClick)
                }

                is CommunityMapsState.Success -> {
                    if (state.maps.isEmpty()) {
                        MoaMapListPlaceholder {
                            EmptyMessage(text = "아직 등록된 지도가 없어요")
                        }
                    } else {
                        state.maps.forEach { map ->
                            CommunityMapCard(
                                map = map,
                                onClick = { onMapClick(map) },
                                imageSize = CommunityCardImageSize,
                                imageGap = CommunityCardImageGap,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 공식 지도 섹션. 시안: 제목 줄 ↔ 카드 16, 카드는 가로로 넘기고 사이 8.
 *
 * 앞 5개만 보여 준다. 나머지는 「전체보기」(공식지도 목록)에서 본다. 로딩·오류·빈 상태는
 * 시안이 없어 커뮤니티 섹션과 같은 모양을 쓴다.
 */
@Composable
private fun OfficialMapSection(
    state: OfficialMapsState,
    onSeeAllClick: () -> Unit,
    onMapClick: (OfficialMap) -> Unit,
    onRetryClick: () -> Unit,
) {
    val screenPadding = Modifier.padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(title = "공식 지도", onSeeAllClick = onSeeAllClick, modifier = screenPadding)

        when (state) {
            OfficialMapsState.Loading -> MoaMapListPlaceholder(screenPadding) {
                CircularProgressIndicator()
            }

            is OfficialMapsState.Error -> MoaMapListPlaceholder(screenPadding) {
                MoaMapErrorNotice(message = state.message, onRetryClick = onRetryClick)
            }

            is OfficialMapsState.Success -> {
                if (state.maps.isEmpty()) {
                    MoaMapListPlaceholder(screenPadding) {
                        EmptyMessage(text = "아직 공식 지도가 없어요")
                    }
                } else {
                    LazyRow(
                        contentPadding = HorizontalListPadding,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.maps, key = { it.id }) { map ->
                            OfficialMapCard(map = map, onClick = { onMapClick(map) })
                        }
                    }
                }
            }
        }
    }
}

/**
 * 홈의 공식 지도 카드. 시안 「탐색탭_지도」: 여백 12, 사진 110(모서리 8) ↔ 이름 8, 이름은 위아래 2 안쪽.
 *
 * 설명은 없고 이름만 사진 폭 안에서 두 줄까지, 넘치면 「…」. 한 줄 이름도 두 줄 자리를 잡아
 * 카드 높이가 같다 - 가로 목록이 들쭉날쭉하지 않다.
 */
@Composable
private fun OfficialMapCard(
    map: OfficialMap,
    onClick: () -> Unit,
) {
    ShadowedSurface(onClick = onClick) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhotoThumbnail(imageUrl = map.imageUrl, size = OfficialCardImageSize)
            Text(
                text = map.title,
                style = MoaMapTheme.typography.body3.withDesignLineHeight(),
                color = MoaMapTheme.colors.textNormal,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(OfficialCardImageSize)
                    .padding(vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = MoaMapTheme.typography.body2,
        color = MoaMapTheme.colors.textAssistive,
    )
}

private fun previewCommunityMaps() = List(3) { index ->
    CommunityMap(
        id = index + 1L,
        title = "서울 팝업스토어 맵",
        imageUrl = null,
        hashtags = listOf("맛집", "데이트코스", "데이트"),
        memberCount = 2312,
        placeCount = 116,
        joined = false,
    )
}

private fun previewOfficialMaps() = List(3) { index ->
    OfficialMap(
        id = index + 100L,
        title = if (index == 0) "공중화장실 지도" else "유동인구 지도",
        description = "행정안전부 공공데이터를 기반으로 전국 공중화장실의 위치를 보여주는 공식 지도입니다.",
        imageUrl = null,
        memberCount = 0,
        placeCount = 0,
        joined = false,
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ExploreScreenPreview() {
    MoaMapTheme {
        ExploreContent(
            uiState = ExploreUiState(
                banners = emptyList(),
                communityMaps = CommunityMapsState.Success(previewCommunityMaps()),
                officialMaps = OfficialMapsState.Success(previewOfficialMaps()),
            ),
            onProfileEditClick = {},
            onSettingsClick = {},
            onNotificationClick = {},
            onCommunityMapClick = {},
            onSeeAllCommunityMapsClick = {},
            onCommunityRetryClick = {},
            onOfficialMapClick = {},
            onSeeAllOfficialMapsClick = {},
            onOfficialRetryClick = {},
        )
    }
}
