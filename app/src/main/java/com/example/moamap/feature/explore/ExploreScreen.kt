package com.example.moamap.feature.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.MapCard
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.mypage.ProfileMenu
import com.example.moamap.feature.mypage.rememberProfileMenuState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

private val ScreenHorizontalPadding = 20.dp

@Immutable
private data class CommunityMapUiModel(
    val id: Long,
    val title: String,
    val description: String,
    val hashtags: List<String>,
    val memberCount: String,
    val placeCount: String,
    val joined: Boolean,
)

// TODO: ViewModel 연결 전까지 사용하는 임시 데이터
private val sampleCommunityMaps = listOf(
    CommunityMapUiModel(
        id = 1L,
        title = "서울 팝업스토어 맵",
        description = "매주 업데이트 되는 서울 팝업스토어 정보, 패션, 아트, 뷰티",
        hashtags = listOf("맛집", "데이트코스", "데이트"),
        memberCount = "2.3천명",
        placeCount = "128곳",
        joined = false,
    ),
    CommunityMapUiModel(
        id = 2L,
        title = "서울 팝업스토어 맵",
        description = "매주 업데이트 되는 서울 팝업스토어 정보, 패션, 아트, 뷰티",
        hashtags = listOf("맛집", "데이트코스", "데이트"),
        memberCount = "2.3천명",
        placeCount = "128곳",
        joined = true,
    ),
    CommunityMapUiModel(
        id = 3L,
        title = "서울 팝업스토어 맵",
        description = "매주 업데이트 되는 서울 팝업스토어 정보, 패션, 아트, 뷰티",
        hashtags = listOf("맛집", "데이트코스", "데이트"),
        memberCount = "2.3천명",
        placeCount = "128곳",
        joined = false,
    ),
    CommunityMapUiModel(
        id = 4L,
        title = "서울 팝업스토어 맵",
        description = "매주 업데이트 되는 서울 팝업스토어 정보, 패션, 아트, 뷰티",
        hashtags = listOf("맛집", "데이트코스", "데이트"),
        memberCount = "2.3천명",
        placeCount = "128곳",
        joined = false,
    ),
)

@Composable
fun ExploreScreen(
    onProfileEditClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOfficialMapClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val profileMenuState = rememberProfileMenuState()
    val hazeState = remember { HazeState() }

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
                .haze(state = hazeState)
                .statusBarsPadding(),
        ) {
            ExploreTopBar(onProfileClick = profileMenuState::show)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                SearchBar(onClick = {})
                OfficialMapBanner(onClick = onOfficialMapClick)
                CategoryChipRow()
                CommunityMapSection()

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
                hazeState = hazeState,
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
                    .padding(top = 5.dp, end = ScreenHorizontalPadding),
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
            .padding(horizontal = ScreenHorizontalPadding, vertical = 4.dp)
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(44.dp),
        color = MoaMapPrimitiveColors.White,
        shadowElevation = 10.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = MoaMapTheme.colors.textAssistive,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "장소, 지도를 검색해보세요",
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MoaMapPrimitiveColors.Yellow100,
        shadowElevation = 5.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
private fun CategoryChipRow() {
    val categories = listOf("전체", "카페", "데이트", "산책", "힙플")
    var selected by rememberSaveable { mutableStateOf(categories.first()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            CategoryChip(
                label = category,
                selected = category == selected,
                onClick = { selected = category },
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
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (selected) MoaMapPrimitiveColors.Gray800 else MoaMapPrimitiveColors.White,
        shadowElevation = 5.dp,
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
private fun CommunityMapSection() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "커뮤니티 지도",
            style = MoaMapTheme.typography.title2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SortOptionRow()
            // TODO: 실제 목록은 ViewModel 연결 시 교체한다.
            sampleCommunityMaps.forEach { communityMap ->
                MapCard(
                    title = communityMap.title,
                    description = communityMap.description,
                    memberCount = communityMap.memberCount,
                    placeCount = communityMap.placeCount,
                    joined = communityMap.joined,
                    hashtags = communityMap.hashtags,
                    onClick = {},
                    onJoinClick = {},
                )
            }
        }
    }
}

@Composable
private fun SortOptionRow() {
    val options = listOf("인기순", "최신순", "추천순")
    var selected by rememberSaveable { mutableStateOf(options.first()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                text = option,
                style = if (isSelected) MoaMapTheme.typography.button2 else MoaMapTheme.typography.button3,
                color = if (isSelected) MoaMapTheme.colors.textNormal else MoaMapTheme.colors.textAssistive,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { selected = option },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ExploreScreenPreview() {
    MoaMapTheme {
        ExploreScreen()
    }
}
