package com.example.moamap.feature.explore

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.BannerShadowBlurRadius
import com.example.moamap.core.designsystem.component.CardShadowBlurRadius
import com.example.moamap.core.designsystem.component.CardShadowColor
import com.example.moamap.core.designsystem.component.ListCardShadowBlurRadius
import com.example.moamap.core.designsystem.component.ListCardShadowColor
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.explore.domain.model.CommunityMap
import com.example.moamap.feature.explore.domain.model.CommunityMapSort
import com.example.moamap.feature.explore.presentation.CommunityMapCard
import com.example.moamap.feature.explore.presentation.CommunityMapsState
import com.example.moamap.feature.explore.presentation.ExploreCategories
import com.example.moamap.feature.explore.presentation.ExploreUiState
import com.example.moamap.feature.explore.presentation.ExploreViewModel
import com.example.moamap.feature.explore.presentation.RecommendedMapCard
import com.example.moamap.feature.mypage.ProfileMenu
import com.example.moamap.feature.mypage.rememberProfileMenuState

/** 섹션 제목은 좌우 여백 안에서 4dp 더 들어간다. */
private val SectionTitlePadding = 4.dp

/** 가로 스크롤 목록이 화면 끝까지 흘러가도록, 여백을 콘텐츠 패딩으로 준다. */
private val HorizontalListPadding =
    PaddingValues(horizontal = MoaMapDimens.ScreenHorizontalPadding)

// TODO: 추천 지도는 서버에 해당 API 가 없어 목데이터로 그린다. 추천 신호 도입 후 교체한다.
private val sampleRecommendedMaps = List(3) { index ->
    CommunityMap(
        id = -(index + 1L),
        title = "서울 팝업스토어 맵",
        imageUrl = null,
        hashtags = listOf("맛집", "데이트코스", "데이트"),
        memberCount = 2312,
        // 추천 카드는 메타 줄을 그리지 않아 이 값이 화면에 나오지 않는다.
        placeCount = 0,
        joined = false,
    )
}

@Composable
fun ExploreScreen(
    onProfileEditClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOfficialMapClick: () -> Unit = {},
    onCommunityMapClick: (CommunityMap) -> Unit = {},
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
        onProfileEditClick = onProfileEditClick,
        onSettingsClick = onSettingsClick,
        onOfficialMapClick = onOfficialMapClick,
        onCommunityMapClick = onCommunityMapClick,
        onCategoryClick = viewModel::selectCategory,
        onSortClick = viewModel::selectSort,
        onRetryClick = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun ExploreContent(
    uiState: ExploreUiState,
    onProfileEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOfficialMapClick: () -> Unit,
    onCommunityMapClick: (CommunityMap) -> Unit,
    onCategoryClick: (String) -> Unit,
    onSortClick: (CommunityMapSort) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profileMenuState = rememberProfileMenuState()

    BackHandler(enabled = profileMenuState.isVisible) {
        profileMenuState.dismiss()
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
            ExploreTopBar(onProfileClick = profileMenuState::show)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SearchBar(onClick = {})
                OfficialMapBanner(onClick = onOfficialMapClick)
                RecommendedMapSection(
                    maps = sampleRecommendedMaps,
                    onMapClick = onCommunityMapClick,
                )
                CommunityMapSection(
                    uiState = uiState,
                    onCategoryClick = onCategoryClick,
                    onSortClick = onSortClick,
                    onMapClick = onCommunityMapClick,
                    onRetryClick = onRetryClick,
                )

                // 바텀 네비게이션에 마지막 카드가 가리지 않도록 확보
                Spacer(Modifier.height(80.dp))
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
            contentDescription = "모아맵",
            modifier = Modifier.size(width = 74.dp, height = 44.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_notifications),
                contentDescription = "알림",
                tint = MoaMapPrimitiveColors.Black,
                modifier = Modifier
                    .size(32.dp)
                    .clickable {},
            )
            Icon(
                painter = painterResource(R.drawable.ic_person),
                contentDescription = "프로필 메뉴",
                tint = MoaMapPrimitiveColors.Black,
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onProfileClick),
            )
        }
    }
}

@Composable
private fun SearchBar(
    onClick: () -> Unit,
) {
    ShadowedSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
            .height(44.dp),
        shape = RoundedCornerShape(1000.dp),
        shadowBlurRadius = CardShadowBlurRadius,
        shadowColor = CardShadowColor,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = MoaMapTheme.colors.textAssistive,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "장소,지도를 검색해보세요",
                style = MoaMapTheme.typography.body2,
                color = MoaMapTheme.colors.textAssistive,
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
    maps: List<CommunityMap>,
    onMapClick: (CommunityMap) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // TODO: 사용자 이름은 프로필 API 연결 시 채운다.
        SectionTitle(text = "00님을 위한 추천 지도")

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

@Composable
private fun CommunityMapSection(
    uiState: ExploreUiState,
    onCategoryClick: (String) -> Unit,
    onSortClick: (CommunityMapSort) -> Unit,
    onMapClick: (CommunityMap) -> Unit,
    onRetryClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(text = "커뮤니티 지도")

        CategoryChipRow(
            selected = uiState.selectedCategory,
            onClick = onCategoryClick,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            SortOptionRow(selected = uiState.sort, onClick = onSortClick)

            when (val state = uiState.communityMaps) {
                CommunityMapsState.Loading -> CommunityMapsPlaceholder {
                    CircularProgressIndicator()
                }

                is CommunityMapsState.Error -> CommunityMapsPlaceholder {
                    ErrorContent(message = state.message, onRetryClick = onRetryClick)
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

/** 목록 자리에 로딩·오류·빈 상태를 같은 높이로 앉혀 화면이 튀지 않게 한다. */
@Composable
private fun CommunityMapsPlaceholder(
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
private fun CategoryChipRow(
    selected: String,
    onClick: (String) -> Unit,
) {
    LazyRow(
        contentPadding = HorizontalListPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ExploreCategories, key = { it }) { category ->
            CategoryChip(
                label = category,
                selected = category == selected,
                onClick = { onClick(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ShadowedSurface(
        shape = RoundedCornerShape(1000.dp),
        color = if (selected) MoaMapPrimitiveColors.Gray800 else MoaMapPrimitiveColors.White,
        shadowBlurRadius = ListCardShadowBlurRadius,
        shadowColor = ListCardShadowColor,
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MoaMapTheme.typography.button3,
            color = if (selected) MoaMapTheme.colors.textWhite else MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SortOptionRow(
    selected: CommunityMapSort,
    onClick: (CommunityMapSort) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CommunityMapSort.entries.forEach { sort ->
            val isSelected = sort == selected
            Text(
                text = sort.label,
                style = if (isSelected) {
                    MoaMapTheme.typography.button2
                } else {
                    MoaMapTheme.typography.button3
                },
                color = if (isSelected) {
                    MoaMapTheme.colors.textNormal
                } else {
                    MoaMapTheme.colors.textAssistive
                },
                maxLines = 1,
                modifier = Modifier.clickable { onClick(sort) },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ExploreScreenPreview() {
    MoaMapTheme {
        ExploreContent(
            uiState = ExploreUiState(
                communityMaps = CommunityMapsState.Success(
                    List(3) { index ->
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
                ),
            ),
            onProfileEditClick = {},
            onSettingsClick = {},
            onOfficialMapClick = {},
            onCommunityMapClick = {},
            onCategoryClick = {},
            onSortClick = {},
            onRetryClick = {},
        )
    }
}
