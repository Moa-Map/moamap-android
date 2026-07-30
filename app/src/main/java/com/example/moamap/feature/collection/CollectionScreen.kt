package com.example.moamap.feature.collection

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.core.designsystem.theme.withDesignLineHeight
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.collection.domain.model.MyMap
import com.example.moamap.feature.collection.presentation.CollectionUiState
import com.example.moamap.feature.collection.presentation.CollectionViewModel
import com.example.moamap.feature.collection.presentation.JoinMapDialog
import com.example.moamap.feature.collection.presentation.JoinState
import com.example.moamap.feature.collection.presentation.MyMapsState
import com.example.moamap.feature.collection.presentation.splitPersonal
import com.example.moamap.core.common.format.formatMemberCount
import com.example.moamap.core.common.format.formatPlaceCount
import com.example.moamap.feature.explore.presentation.MapThumbnail

/** 카드 썸네일과 같은 높이를 유지해 제목/메타가 위아래로 벌어지도록 한다. */
private val CardThumbnailSize = 64.dp

/** 시안의 "장소 이미지" 프레임과 같은 값. */
private val CardThumbnailShape = RoundedCornerShape(4.dp)

/** 인스타그램 브랜드 색. 디자인 시스템 팔레트가 아니라서 토큰으로 승격하지 않는다. */
private val InstagramCardBackground = Color(0xFFFFF5FB)
private val InstagramCardTitle = Color(0xFFAF0069)

/** 프라이빗 탭 상단 액션 카드 높이. */
private val ActionCardHeight = 72.dp

/** 목록 자리에 로딩·오류·빈 상태를 같은 높이로 앉혀 화면이 튀지 않게 한다. */
private val ListPlaceholderHeight = 200.dp

private val MapType.label: String
    get() = when (this) {
        MapType.Community -> "커뮤니티"
        MapType.Private -> "프라이빗"
    }

@Immutable
internal data class CollectionMapUiModel(
    val id: Long,
    val title: String,
    /** 커버 이미지 주소. null 이면 [MapThumbnail] 이 기본 이미지를 그린다. */
    val imageUrl: String? = null,
    /**
     * 등록 장소 수.
     *
     * 서버 목록 응답에 해당 필드가 없어 지금은 채우지 않는다. null 이면 표시하지 않는다.
     */
    val placeCount: String? = null,
    val verified: Boolean = false,
    /** null 이면 인원 수를 노출하지 않는다. */
    val memberCount: String? = null,
)

private fun MyMap.toCommunityUiModel() = CollectionMapUiModel(
    id = id,
    title = title,
    imageUrl = imageUrl,
    placeCount = formatPlaceCount(placeCount),
    verified = official,
    memberCount = formatMemberCount(memberCount),
)

/** 프라이빗 카드는 장소 수만 보여주는 자리다. 인원 수는 시안에 없다. */
internal fun MyMap.toPrivateUiModel() = CollectionMapUiModel(
    id = id,
    title = title,
    imageUrl = imageUrl,
    placeCount = formatPlaceCount(placeCount),
    verified = official,
)

@Composable
fun CollectionScreen(
    onNewMapClick: () -> Unit = {},
    onInstagramImportClick: () -> Unit = {},
    onMapShareImportClick: () -> Unit = {},
    onMapClick: (MyMap) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 지도를 만들고 돌아오면 목록이 만들기 전 그대로다. 화면이 다시 보일 때 현재 탭을 다시 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    val join = uiState.join
    if (join is JoinState.Editing) {
        JoinMapDialog(
            state = join,
            onCodeChange = viewModel::updateInviteCode,
            onSubmit = viewModel::join,
            onDismiss = viewModel::closeJoinDialog,
        )
    }

    CollectionContent(
        uiState = uiState,
        onTabClick = viewModel::selectTab,
        onRetryClick = viewModel::retry,
        onInviteCodeClick = viewModel::openJoinDialog,
        onNewMapClick = onNewMapClick,
        onInstagramImportClick = onInstagramImportClick,
        onMapShareImportClick = onMapShareImportClick,
        onMapClick = onMapClick,
        modifier = modifier,
    )
}

@Composable
private fun CollectionContent(
    uiState: CollectionUiState,
    onTabClick: (MapType) -> Unit,
    onRetryClick: () -> Unit,
    onInviteCodeClick: () -> Unit,
    onNewMapClick: () -> Unit,
    onInstagramImportClick: () -> Unit,
    onMapShareImportClick: () -> Unit,
    onMapClick: (MyMap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedTab = uiState.selectedTab

    // 탭마다 스크롤 위치를 따로 기억해, 탭을 오갈 때 보던 자리로 돌아온다.
    val communityScrollState = rememberScrollState()
    val privateScrollState = rememberScrollState()
    val scrollState = when (selectedTab) {
        MapType.Community -> communityScrollState
        MapType.Private -> privateScrollState
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
                onTabClick = onTabClick,
            )

            when (selectedTab) {
                MapType.Community -> CommunityTabContent(
                    state = uiState.community,
                    onRetryClick = onRetryClick,
                    onMapClick = onMapClick,
                )

                MapType.Private -> PrivateTabContent(
                    state = uiState.private,
                    onRetryClick = onRetryClick,
                    onInstagramImportClick = onInstagramImportClick,
                    onMapShareImportClick = onMapShareImportClick,
                    onMapClick = onMapClick,
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
    selectedTab: MapType,
    onTabClick: (MapType) -> Unit,
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
        MapType.entries.forEach { tab ->
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
private fun CommunityTabContent(
    state: MyMapsState,
    onRetryClick: () -> Unit,
    onMapClick: (MyMap) -> Unit,
) {
    MapsStateContent(
        state = state,
        emptyMessage = "아직 참여한 지도가 없어요",
        onRetryClick = onRetryClick,
    ) { maps ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            maps.forEach { map ->
                CollectionMapCard(
                    map = map.toCommunityUiModel(),
                    onClick = { onMapClick(map) },
                )
            }
        }
    }
}

@Composable
private fun PrivateTabContent(
    state: MyMapsState,
    onRetryClick: () -> Unit,
    onInstagramImportClick: () -> Unit,
    onMapShareImportClick: () -> Unit,
    onMapClick: (MyMap) -> Unit,
) {
    // 액션 카드는 목록 상태와 무관하게 늘 보인다. 목록이 비었을 때야말로 만들 진입점이 필요하다.
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PrivateActionCards(
            onInstagramImportClick = onInstagramImportClick,
            onMapShareImportClick = onMapShareImportClick,
        )

        MapsStateContent(
            state = state,
            emptyMessage = "아직 만든 지도가 없어요",
            onRetryClick = onRetryClick,
        ) { maps ->
            val sections = maps.splitPersonal()
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PrivateMapSection("나만의 지도", sections.personal, onMapClick)
                PrivateMapSection("전체", sections.others, onMapClick)
            }
        }
    }
}

/**
 * 목록 자리의 로딩·오류·빈 상태를 한곳에서 그린다.
 *
 * 세 상태 모두 같은 높이를 차지해, 상태가 바뀔 때 화면이 튀지 않는다.
 */
@Composable
internal fun MapsStateContent(
    state: MyMapsState,
    emptyMessage: String,
    onRetryClick: () -> Unit,
    content: @Composable (List<MyMap>) -> Unit,
) {
    when (state) {
        MyMapsState.Loading -> ListPlaceholder { CircularProgressIndicator() }

        is MyMapsState.Error -> ListPlaceholder {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = state.message,
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MoaMapPrimitiveColors.Blue500,
                    onClick = onRetryClick,
                ) {
                    Text(
                        text = "다시 시도",
                        style = MoaMapTheme.typography.button2,
                        color = MoaMapTheme.colors.textWhite,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        is MyMapsState.Success -> {
            if (state.maps.isEmpty()) {
                ListPlaceholder {
                    Text(
                        text = emptyMessage,
                        style = MoaMapTheme.typography.body2,
                        color = MoaMapTheme.colors.textAssistive,
                    )
                }
            } else {
                content(state.maps)
            }
        }
    }
}

@Composable
private fun ListPlaceholder(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ListPlaceholderHeight),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PrivateActionCards(
    onInstagramImportClick: () -> Unit,
    onMapShareImportClick: () -> Unit,
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
        PrivateActionCard(
            iconRes = R.drawable.ic_map,
            title = "외부 지도",
            subtitle = "불러오기",
            backgroundColor = MoaMapPrimitiveColors.Yellow50,
            titleColor = MoaMapPrimitiveColors.Yellow800,
            iconTint = MoaMapPrimitiveColors.Yellow500,
            onClick = onMapShareImportClick,
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

/** 목록이 비어도 제목은 남긴다. 자리가 사라졌다 나타나면 화면 구성이 바뀌어 보인다. */
@Composable
private fun PrivateMapSection(
    title: String,
    maps: List<MyMap>,
    onMapClick: (MyMap) -> Unit,
) {
    // 제목만 떠 있고 아래가 비어 있으면 못 불러온 것처럼 보인다.
    if (maps.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MoaMapTheme.typography.title2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            maps.forEach { map ->
                CollectionMapCard(
                    map = map.toPrivateUiModel(),
                    onClick = { onMapClick(map) },
                )
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
    border: BorderStroke? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MoaMapPrimitiveColors.White,
        border = border,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MapThumbnail(
                imageUrl = map.imageUrl,
                size = CardThumbnailSize,
                shape = CardThumbnailShape,
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
                // 인원 수가 있는 카드와 없는 카드가 아이콘 크기·간격이 다르다.
                // 둘 다 없으면 메타 줄을 그리지 않고 자리를 비워 둔다.
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
                        if (map.placeCount != null) {
                            CollectionMapMeta(
                                iconRes = R.drawable.ic_location,
                                text = map.placeCount,
                                contentDescription = "등록 장소",
                                iconSize = 14.dp,
                                gap = 2.dp,
                            )
                        }
                    }
                } else if (map.placeCount != null) {
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
        CollectionContent(
            uiState = CollectionUiState(
                community = MyMapsState.Success(
                    listOf(
                        MyMap(1L, "서울 팝업스토어 맵", null, 2312, 116, official = false, personal = false),
                        MyMap(2L, "성수 카페 투어", null, 24, 0, official = false, personal = false),
                    ),
                ),
            ),
            onTabClick = {},
            onRetryClick = {},
            onInviteCodeClick = {},
            onNewMapClick = {},
            onInstagramImportClick = {},
            onMapShareImportClick = {},
            onMapClick = {},
        )
    }
}
