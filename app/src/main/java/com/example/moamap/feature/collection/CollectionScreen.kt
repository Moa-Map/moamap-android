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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.core.designsystem.theme.withDesignLineHeight

/** 카드 썸네일과 같은 높이를 유지해 제목/메타가 위아래로 벌어지도록 한다. */
private val CardThumbnailSize = 64.dp

/** 인스타그램 브랜드 색. 디자인 시스템 팔레트가 아니라서 토큰으로 승격하지 않는다. */
private val InstagramCardBackground = Color(0xFFFFF5FB)
private val InstagramCardTitle = Color(0xFFAF0069)

/** 프라이빗 탭 상단 액션 카드 높이. */
private val ActionCardHeight = 72.dp

private enum class CollectionTab(val label: String) {
    Community("커뮤니티"),
    Private("프라이빗"),
}

@Immutable
internal data class CollectionMapUiModel(
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
    onInstagramImportClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(CollectionTab.Community) }

    // 탭마다 스크롤 위치를 따로 기억해, 탭을 오갈 때 보던 자리로 돌아온다.
    val communityScrollState = rememberScrollState()
    val privateScrollState = rememberScrollState()
    val scrollState = when (selectedTab) {
        CollectionTab.Community -> communityScrollState
        CollectionTab.Private -> privateScrollState
    }

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
                .verticalScroll(scrollState)
                .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            CollectionTabRow(
                selectedTab = selectedTab,
                onTabClick = { selectedTab = it },
            )

            when (selectedTab) {
                CollectionTab.Community -> CommunityTabContent()
                CollectionTab.Private -> PrivateTabContent(
                    onInstagramImportClick = onInstagramImportClick,
                )
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
            .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
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
            .padding(4.dp)
            .selectableGroup(),
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
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onTabClick(tab) },
                    )
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
private fun PrivateTabContent(
    onInstagramImportClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PrivateActionCards(onInstagramImportClick = onInstagramImportClick)

        // TODO: 실제 목록은 ViewModel 연결 시 교체한다.
        PrivateMapSection(title = "나만의 지도", maps = sampleMyMaps)
        PrivateMapSection(title = "전체", maps = sampleAllPrivateMaps)
    }
}

@Composable
private fun PrivateActionCards(
    onInstagramImportClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PrivateActionCard(
            iconRes = R.drawable.ic_instagram_logo,
            title = "인스타그램",
            subtitle = "장소 찾기",
            backgroundColor = InstagramCardBackground,
            titleColor = InstagramCardTitle,
            // 브랜드 로고라 원본 색을 그대로 쓴다.
            iconTint = Color.Unspecified,
            onClick = onInstagramImportClick,
            modifier = Modifier.weight(1f),
        )
        // TODO: 외부 지도 불러오기는 다음 이슈에서 연결한다.
        PrivateActionCard(
            iconRes = R.drawable.ic_map,
            title = "외부 지도",
            subtitle = "불러오기",
            backgroundColor = MoaMapPrimitiveColors.Yellow50,
            titleColor = MoaMapPrimitiveColors.Yellow800,
            iconTint = MoaMapPrimitiveColors.Yellow500,
            onClick = {},
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PrivateActionCard(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    titleColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.height(ActionCardHeight),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                // 두 줄 사이 간격은 피그마에도 gap 이 없고 line height 로만 벌어진다.
                // 기본 Trim 을 끄지 않으면 디자인보다 줄이 붙는다.
                Text(
                    text = title,
                    style = MoaMapTheme.typography.subtitle2.withDesignLineHeight(),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MoaMapTheme.typography.body1.withDesignLineHeight(),
                    color = MoaMapTheme.colors.textNormal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_outward),
                contentDescription = null,
                tint = MoaMapPrimitiveColors.Black,
                modifier = Modifier.size(24.dp),
            )
        }
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

/**
 * 모음 지도 카드.
 *
 * 장소 가져오기의 지도 선택 화면도 같은 카드를 쓰므로, 선택 표시 같은 우측 요소는
 * [trailingContent] 슬롯으로 받는다.
 */
@Composable
internal fun CollectionMapCard(
    map: CollectionMapUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MoaMapPrimitiveColors.White,
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

            trailingContent?.invoke()
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
