package com.example.moamap.feature.collection

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val ScreenHorizontalPadding = 20.dp

/** 카드 썸네일과 같은 높이를 유지해 제목/메타가 위아래로 벌어지도록 한다. */
private val CardThumbnailSize = 64.dp

private enum class CollectionTab(val label: String) {
    Community("커뮤니티"),
    Private("프라이빗"),
}

@Immutable
private data class CollectionMapUiModel(
    val id: Long,
    val title: String,
    val placeCount: String,
    val verified: Boolean = false,
    /** null 이면 인원 수를 노출하지 않는다. */
    val memberCount: String? = null,
)

// TODO: ViewModel 연결 전까지 사용하는 임시 데이터
private val sampleCommunityMaps = listOf(
    CollectionMapUiModel(id = 1L, title = "화장실", placeCount = "128곳", verified = true),
    CollectionMapUiModel(id = 2L, title = "서울 팝업스토어 맵", placeCount = "128곳", memberCount = "2.3천명"),
    CollectionMapUiModel(id = 3L, title = "화장실", placeCount = "128곳", verified = true),
    CollectionMapUiModel(id = 4L, title = "서울 팝업스토어 맵", placeCount = "128곳", memberCount = "2.3천명"),
    CollectionMapUiModel(id = 5L, title = "서울 팝업스토어 맵", placeCount = "128곳", memberCount = "2.3천명"),
)

private val sampleMyMaps = listOf(
    CollectionMapUiModel(id = 11L, title = "내 지도", placeCount = "128곳"),
)

private val sampleAllPrivateMaps = List(3) { index ->
    CollectionMapUiModel(id = 21L + index, title = "내 지도", placeCount = "128곳")
}

@Composable
fun CollectionScreen(
    onInviteCodeClick: () -> Unit = {},
    onNewMapClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(CollectionTab.Community) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundPrimary)
            .statusBarsPadding(),
    ) {
        CollectionTopBar(
            onInviteCodeClick = onInviteCodeClick,
            onNewMapClick = onNewMapClick,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            CollectionTabRow(
                selectedTab = selectedTab,
                onTabClick = { selectedTab = it },
            )

            when (selectedTab) {
                CollectionTab.Community -> CommunityTabContent()
                CollectionTab.Private -> PrivateTabContent()
            }

            // 바텀 네비게이션에 마지막 카드가 가리지 않도록 확보
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun CollectionTopBar(
    onInviteCodeClick: () -> Unit,
    onNewMapClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = ScreenHorizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.img_moa_logo),
            contentDescription = "모아맵",
            modifier = Modifier.size(width = 74.dp, height = 44.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onInviteCodeClick),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_key),
                    contentDescription = null,
                    tint = MoaMapPrimitiveColors.Black,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "초대 코드",
                    style = MoaMapTheme.typography.button2,
                    color = MoaMapPrimitiveColors.Black,
                )
            }
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = MoaMapPrimitiveColors.Blue500,
                onClick = onNewMapClick,
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 8.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        tint = MoaMapTheme.colors.textWhite,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "새 지도",
                        style = MoaMapTheme.typography.button2,
                        color = MoaMapTheme.colors.textWhite,
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionTabRow(
    selectedTab: CollectionTab,
    onTabClick: (CollectionTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .background(MoaMapPrimitiveColors.Yellow50)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CollectionTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        if (isSelected) {
                            MoaMapPrimitiveColors.Yellow100
                        } else {
                            MoaMapPrimitiveColors.Yellow50
                        },
                    )
                    .clickable { onTabClick(tab) }
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    style = if (isSelected) {
                        MoaMapTheme.typography.subtitle3
                    } else {
                        MoaMapTheme.typography.subtitle2
                    },
                    color = if (isSelected) {
                        MoaMapPrimitiveColors.Yellow900
                    } else {
                        MoaMapTheme.colors.textAssistive
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CommunityTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // TODO: 실제 목록은 ViewModel 연결 시 교체한다.
        sampleCommunityMaps.forEach { map ->
            CollectionMapCard(map = map, onClick = {})
        }
    }
}

@Composable
private fun PrivateTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // TODO: 실제 목록은 ViewModel 연결 시 교체한다.
        PrivateMapSection(title = "나만의 지도", maps = sampleMyMaps)
        PrivateMapSection(title = "전체", maps = sampleAllPrivateMaps)
    }
}

@Composable
private fun PrivateMapSection(
    title: String,
    maps: List<CollectionMapUiModel>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MoaMapTheme.typography.title2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            maps.forEach { map ->
                CollectionMapCard(map = map, onClick = {})
            }
        }
    }
}

@Composable
private fun CollectionMapCard(
    map: CollectionMapUiModel,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MoaMapPrimitiveColors.White,
        shadowElevation = 5.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // TODO: 지도 썸네일 이미지는 데이터 연결 시 채운다.
            Box(
                modifier = Modifier
                    .size(CardThumbnailSize)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MoaMapPrimitiveColors.Yellow50),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(CardThumbnailSize)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = map.title,
                        style = MoaMapTheme.typography.subtitle2,
                        color = MoaMapPrimitiveColors.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (map.verified) {
                        Icon(
                            painter = painterResource(R.drawable.ic_verify_filled),
                            contentDescription = "공식 인증",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (map.memberCount != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CollectionMapMeta(
                            iconRes = R.drawable.ic_person,
                            text = map.memberCount,
                            contentDescription = "참여 인원",
                            iconSize = 14.dp,
                            gap = 2.dp,
                        )
                        CollectionMapMeta(
                            iconRes = R.drawable.ic_location,
                            text = map.placeCount,
                            contentDescription = "등록 장소",
                            iconSize = 14.dp,
                            gap = 2.dp,
                        )
                    }
                } else {
                    CollectionMapMeta(
                        iconRes = R.drawable.ic_location,
                        text = map.placeCount,
                        contentDescription = "등록 장소",
                        iconSize = 12.dp,
                        gap = 4.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionMapMeta(
    @DrawableRes iconRes: Int,
    text: String,
    contentDescription: String,
    iconSize: Dp,
    gap: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MoaMapTheme.colors.textAssistive,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = text,
            style = MoaMapTheme.typography.caption0,
            color = MoaMapTheme.colors.textAssistive,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun CollectionScreenPreview() {
    MoaMapTheme {
        CollectionScreen()
    }
}
