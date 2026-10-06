package com.moamap.app.feature.collection

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.common.format.formatMemberCount
import com.moamap.app.core.common.format.formatPlaceCount
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.component.MoaMapTopBarLogo
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.presentation.CollectionEditState
import com.moamap.app.feature.collection.presentation.CollectionUiState
import com.moamap.app.feature.collection.presentation.CollectionViewModel
import com.moamap.app.feature.collection.presentation.JoinMapDialog
import com.moamap.app.feature.collection.presentation.JoinState
import com.moamap.app.feature.collection.presentation.LeaveEligibility
import com.moamap.app.feature.collection.presentation.MyMapsState
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportCheckBox
import com.moamap.app.feature.collection.presentation.placeimport.selectedCardBorder
import com.moamap.app.feature.collection.presentation.splitPersonal

/** 카드 썸네일과 같은 높이를 유지해 제목/메타가 위아래로 벌어지도록 한다. */
private val CardThumbnailSize = 64.dp

/** 모음 카드끼리의 간격. 순서를 바꿀 때 이웃을 넘었는지 재는 데도 쓴다. */
private val CardSpacing = 8.dp

/** 인스타그램 브랜드 색. 디자인 시스템 팔레트가 아니라서 토큰으로 승격하지 않는다. */
private val InstagramCardBackground = Color(0xFFFFF5FB)
private val InstagramCardTitle = Color(0xFFAF0069)

/** 프라이빗 탭 상단 액션 카드 높이. */
private val ActionCardHeight = 72.dp

/** 목록 자리에 로딩·오류·빈 상태를 같은 높이로 앉혀 화면이 튀지 않게 한다. */
private val ListPlaceholderHeight = 200.dp

/** 편집에서 지도를 고르면 하단 탭 대신 뜨는 막대. 시안 `1974:8444`. */
private val SelectionBarHeight = 75.dp

private val CollectionTabs = listOf(MapType.Community, MapType.Private)

private val MapType.label: String
    get() = when (this) {
        MapType.Community -> "커뮤니티"
        MapType.Private -> "프라이빗"
        MapType.Official -> "공식"
    }

@Immutable
internal data class CollectionMapUiModel(
    val id: Long,
    val title: String,
    /** 커버 이미지 주소. null 이면 [PhotoThumbnail] 이 기본 사진을 그린다. */
    val imageUrl: String? = null,
    /** 등록 장소 수. null 이면 그 자리를 그리지 않는다. */
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
    onHomeClick: () -> Unit = {},
    onNewMapClick: () -> Unit = {},
    onInstagramImportClick: () -> Unit = {},
    onMapShareImportClick: () -> Unit = {},
    onMapClick: (MyMap) -> Unit = {},
    /** 아래 선택 막대가 떴는지. 뜬 동안 하단 탭을 숨겨야 한다 - 탭은 NavHost 가 그린다. */
    onSelectionBarVisibleChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val edit = uiState.edit

    val selectionBarVisible = edit?.selectionBarVisible == true
    LaunchedEffect(selectionBarVisible) { onSelectionBarVisibleChange(selectionBarVisible) }
    DisposableEffect(Unit) { onDispose { onSelectionBarVisibleChange(false) } }

    // 편집 중이면 기기 뒤로가기는 편집부터 끝낸다.
    BackHandler(enabled = edit != null) { viewModel.finishEdit() }

    if (edit?.confirmVisible == true) {
        MoaMapConfirmDialog(
            title = "${edit.selected.size}개의 지도",
            titleSuffix = "에서 나가시겠습니까?",
            message = "나가면 모음 탭에서 지도가 사라집니다",
            confirmText = "나가기",
            onConfirm = viewModel::leaveSelected,
            onDismissRequest = viewModel::closeLeaveConfirm,
            dismissText = "닫기",
            dismissColor = MoaMapPrimitiveColors.Gray200,
        )
    }

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
        onHomeClick = onHomeClick,
        onInviteCodeClick = viewModel::openJoinDialog,
        onNewMapClick = onNewMapClick,
        onInstagramImportClick = onInstagramImportClick,
        onMapShareImportClick = onMapShareImportClick,
        onMapClick = { map ->
            // 편집 중에는 카드를 누르면 고른다. 지도로 들어가지 않는다.
            if (edit != null) viewModel.toggleSelection(map.id) else onMapClick(map)
        },
        onEditClick = { if (edit != null) viewModel.finishEdit() else viewModel.startEdit() },
        onMapMove = viewModel::moveMap,
        onLeaveClick = viewModel::openLeaveConfirm,
        onNoticeShown = viewModel::consumeNotice,
        modifier = modifier,
    )
}

@Composable
private fun CollectionContent(
    uiState: CollectionUiState,
    onTabClick: (MapType) -> Unit,
    onRetryClick: () -> Unit,
    onHomeClick: () -> Unit,
    onInviteCodeClick: () -> Unit,
    onNewMapClick: () -> Unit,
    onInstagramImportClick: () -> Unit,
    onMapShareImportClick: () -> Unit,
    onMapClick: (MyMap) -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    onMapMove: (mapId: Long, targetId: Long) -> Unit = { _, _ -> },
    onLeaveClick: () -> Unit = {},
    onNoticeShown: () -> Unit = {},
) {
    val selectedTab = uiState.selectedTab
    val edit = uiState.edit

    // 탭마다 스크롤 위치를 따로 기억해, 탭을 오갈 때 보던 자리로 돌아온다.
    val communityScrollState = rememberScrollState()
    val privateScrollState = rememberScrollState()
    val scrollState = when (selectedTab) {
        MapType.Community -> communityScrollState
        MapType.Private -> privateScrollState
        MapType.Official -> communityScrollState
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundPrimary)
                .statusBarsPadding(),
        ) {
            CollectionTopBar(
                onHomeClick = onHomeClick,
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
                        edit = edit,
                        onRetryClick = onRetryClick,
                        onMapClick = onMapClick,
                        onEditClick = onEditClick,
                        onMapMove = onMapMove,
                    )

                    MapType.Private -> PrivateTabContent(
                        state = uiState.private,
                        edit = edit,
                        onRetryClick = onRetryClick,
                        onInstagramImportClick = onInstagramImportClick,
                        onMapShareImportClick = onMapShareImportClick,
                        onMapClick = onMapClick,
                        onEditClick = onEditClick,
                        onMapMove = onMapMove,
                    )

                    // 탭이 없는 종류다. [CollectionTabs] 참고.
                    MapType.Official -> Unit
                }

                // 바텀 네비게이션에 마지막 카드가 가리지 않도록 확보
                Spacer(Modifier.height(80.dp))
            }
        }

        // 안내는 하단 탭(또는 선택 막대) 위에 쌓는다.
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            ErrorSnackbar(message = uiState.notice, onShown = onNoticeShown)
            if (edit != null && edit.selectionBarVisible) {
                SelectionBar(
                    count = edit.selected.size,
                    leaving = edit.leaving,
                    onLeaveClick = onLeaveClick,
                )
            } else {
                // 하단 탭 자리. 탭은 NavHost 가 그린다.
                Spacer(Modifier.navigationBarsPadding().height(80.dp))
            }
        }
    }
}

/**
 * 고른 지도 수와 나가기. 시안 `1974:8444`. 하단 탭 자리를 대신한다.
 */
@Composable
private fun SelectionBar(count: Int, leaving: Boolean, onLeaveClick: () -> Unit) {
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MoaMapPrimitiveColors.White,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .height(SelectionBarHeight)
                .padding(horizontal = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${count}개 선택됨",
                style = MoaMapTheme.typography.subtitle2,
                color = MoaMapTheme.colors.textNormal,
            )
            Text(
                text = "나가기",
                style = MoaMapTheme.typography.button2,
                color = MoaMapTheme.colors.textWhite,
                modifier = Modifier
                    .clip(RoundedCornerShape(1000.dp))
                    .background(MoaMapTheme.colors.statusAlert)
                    .clickable(enabled = !leaving, onClick = onLeaveClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CollectionTopBar(
    onHomeClick: () -> Unit,
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
        MoaMapTopBarLogo(onClick = onHomeClick)
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
        CollectionTabs.forEach { tab ->
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
    edit: CollectionEditState?,
    onRetryClick: () -> Unit,
    onMapClick: (MyMap) -> Unit,
    onEditClick: () -> Unit,
    onMapMove: (mapId: Long, targetId: Long) -> Unit,
) {
    MapsStateContent(
        state = state,
        emptyMessage = "아직 참여한 지도가 없어요",
        onRetryClick = onRetryClick,
    ) { maps ->
        val reorder = rememberMapReorderState(ids = maps.map { map -> map.id }, spacing = CardSpacing, onMove = onMapMove)
        // 시안 `1974:7318`: 목록 오른쪽 위에 「편집」, 목록과 12. 폭을 채워야 「편집」이 오른쪽 끝에 붙는다.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditToggle(edit = edit, onClick = onEditClick, modifier = Modifier.align(Alignment.End))
            Column(verticalArrangement = Arrangement.spacedBy(CardSpacing)) {
                // 순서를 바꾸면 카드가 자리째 옮겨가야 끌던 손잡이의 동작이 끊기지 않는다.
                maps.forEach { map ->
                    key(map.id) {
                        MyMapCard(
                            map = map,
                            uiModel = map.toCommunityUiModel(),
                            edit = edit,
                            reorder = reorder,
                            onClick = { onMapClick(map) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivateTabContent(
    state: MyMapsState,
    edit: CollectionEditState?,
    onRetryClick: () -> Unit,
    onInstagramImportClick: () -> Unit,
    onMapShareImportClick: () -> Unit,
    onMapClick: (MyMap) -> Unit,
    onEditClick: () -> Unit,
    onMapMove: (mapId: Long, targetId: Long) -> Unit,
) {
    // 액션 카드는 목록 상태와 무관하게 늘 보인다. 목록이 비었을 때야말로 만들 진입점이 필요하다.
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PrivateActionCards(
            onInstagramImportClick = onInstagramImportClick,
            onMapShareImportClick = onMapShareImportClick,
        )

        // 목록이 비어도 「프라이빗 지도」 제목과 「편집」은 보이고, 그 아래에서 비었다고 알린다.
        MapsStateContent(
            state = state,
            emptyMessage = null,
            onRetryClick = onRetryClick,
        ) { maps ->
            val sections = maps.splitPersonal()
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // 나만의 지도는 나갈 대상이 아니라 편집해도 체크박스를 달지 않는다.
                PrivateMapSection("나만의 지도", sections.personal, edit = null, onMapClick = onMapClick)
                // 시안 `1976:8630`: 「편집」은 이 섹션 제목 아래, 목록 바로 위에 있다.
                PrivateMapSection(
                    title = "프라이빗 지도",
                    maps = sections.others,
                    edit = edit,
                    onMapClick = onMapClick,
                    onMapMove = onMapMove,
                    emptyMessage = "참여하고 있는 프라이빗 지도가 없습니다",
                    editToggle = {
                        EditToggle(edit = edit, onClick = onEditClick, modifier = Modifier.align(Alignment.End))
                    },
                )
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
    /** null 이면 목록이 비어도 [content] 를 그린다. 빈 상태를 화면이 직접 그릴 때다. */
    emptyMessage: String?,
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
            if (state.maps.isEmpty() && emptyMessage != null) {
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
        ImportActionCard(
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
        ImportActionCard(
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
internal fun ImportActionCard(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    titleColor: Color,
    iconTint: Color,
    onClick: (() -> Unit)?,
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
    edit: CollectionEditState?,
    onMapClick: (MyMap) -> Unit,
    onMapMove: (mapId: Long, targetId: Long) -> Unit = { _, _ -> },
    /** 있으면 지도가 없어도 섹션을 그리고 이 문구로 비었다고 알린다. 없으면 섹션째 숨긴다. */
    emptyMessage: String? = null,
    editToggle: (@Composable ColumnScope.() -> Unit)? = null,
) {
    // 제목만 떠 있고 아래가 비어 있으면 못 불러온 것처럼 보인다.
    if (maps.isEmpty() && emptyMessage == null) return

    val reorder = rememberMapReorderState(ids = maps.map { map -> map.id }, spacing = CardSpacing, onMove = onMapMove)
    // 시안: 제목과 목록 사이 12, 「편집」과 카드·카드끼리는 8.
    // 폭을 채워야 「편집」이 오른쪽 끝에 붙는다. 카드가 없으면 안내 문구 폭으로 줄어든다.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MoaMapTheme.typography.title2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CardSpacing),
        ) {
            editToggle?.invoke(this)
            if (maps.isEmpty() && emptyMessage != null) {
                Text(
                    text = emptyMessage,
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            // 순서를 바꾸면 카드가 자리째 옮겨가야 끌던 손잡이의 동작이 끊기지 않는다.
            maps.forEach { map ->
                key(map.id) {
                    MyMapCard(
                        map = map,
                        uiModel = map.toPrivateUiModel(),
                        edit = edit,
                        reorder = reorder,
                        onClick = { onMapClick(map) },
                    )
                }
            }
        }
    }
}

/** 「편집」·「완료」. 시안에서 목록 오른쪽 위에 글자만 있다. */
@Composable
private fun EditToggle(edit: CollectionEditState?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = if (edit != null) "완료" else "편집",
        style = MoaMapTheme.typography.button1,
        color = MoaMapTheme.colors.textNormal,
        modifier = modifier.clickable(enabled = edit?.leaving != true, onClick = onClick),
    )
}

/**
 * 모음 목록의 카드. 시안 `1974:7318`·`1974:7468`.
 *
 * 편집 중에만 순서 손잡이가 보이고, 손잡이를 끌어 순서를 바꾼다. 순서 저장은 서버가 준비되면 붙인다.
 * 편집 중에는 체크박스가 붙고 누르는 동안 회색이 된다. 고를 수 없는 지도는 체크박스가 회색이다.
 */
@Composable
private fun MyMapCard(
    map: MyMap,
    uiModel: CollectionMapUiModel,
    edit: CollectionEditState?,
    reorder: MapReorderState,
    onClick: () -> Unit,
) {
    val selected = edit != null && map.id in edit.selected
    CollectionMapCard(
        map = uiModel,
        onClick = onClick,
        modifier = Modifier.reorderableItem(reorder, map.id),
        border = selectedCardBorder(selected),
        reorderable = true,
        showDragHandle = edit != null,
        dragHandleModifier = Modifier.reorderHandle(reorder, map.id),
        pressFeedback = edit != null,
        trailingContent = edit?.let { current ->
            {
                // 확인 중인 지도는 고를 수 있는 모양으로 둔다. 곧 대부분 고를 수 있게 된다.
                val eligibility = current.eligibilityOf(map.id)
                PlaceImportCheckBox(
                    checked = selected,
                    enabled = eligibility == LeaveEligibility.Allowed ||
                        eligibility == LeaveEligibility.Checking,
                )
            }
        },
    )
}

/**
 * 모음 지도 카드.
 *
 * 장소 가져오기의 지도 선택 화면도 같은 카드를 쓰므로, 선택 표시 같은 우측 요소는
 * [trailingContent] 슬롯으로 받는다.
 *
 * @param reorderable 순서를 바꿀 수 있는 모음 목록의 카드다.
 * @param showDragHandle 왼쪽에 순서 손잡이를 둔다. 손잡이가 들면 왼쪽 여백이 12 로 준다.
 * @param dragHandleModifier 손잡이에 다는 끌기 동작.
 * @param pressFeedback 누르는 동안 회색(Gray50)으로 바꾼다. 앱은 누름 효과를 꺼 두었지만
 *  모음 편집은 시안(`1974:8284`)에 있다.
 */
@Composable
internal fun CollectionMapCard(
    map: CollectionMapUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    reorderable: Boolean = false,
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    pressFeedback: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (pressFeedback && pressed) MoaMapPrimitiveColors.Gray50 else MoaMapPrimitiveColors.White,
        border = border,
    ) {
        Row(
            modifier = Modifier
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(
                    start = if (showDragHandle) 12.dp else 16.dp,
                    end = 16.dp,
                    top = if (reorderable) 16.dp else 20.dp,
                    bottom = if (reorderable) 16.dp else 20.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showDragHandle) {
                Icon(
                    painter = painterResource(R.drawable.ic_drag_handle),
                    contentDescription = null,
                    tint = MoaMapTheme.colors.textNormal,
                    modifier = dragHandleModifier.size(24.dp),
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhotoThumbnail(imageUrl = map.imageUrl, size = CardThumbnailSize)
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
            onHomeClick = {},
            onInviteCodeClick = {},
            onNewMapClick = {},
            onInstagramImportClick = {},
            onMapShareImportClick = {},
            onMapClick = {},
        )
    }
}

/** 편집 중: 1번을 골랐고, 2번은 방장이라 고를 수 없다. */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun CollectionScreenEditPreview() {
    MoaMapTheme {
        CollectionContent(
            uiState = CollectionUiState(
                community = MyMapsState.Success(
                    listOf(
                        MyMap(1L, "서울 팝업스토어 맵", null, 2312, 116, official = false, personal = false),
                        MyMap(2L, "성수 카페 투어", null, 24, 0, official = false, personal = false),
                    ),
                ),
                edit = CollectionEditState(
                    eligibility = mapOf(1L to LeaveEligibility.Allowed, 2L to LeaveEligibility.Owner),
                    selected = setOf(1L),
                ),
            ),
            onTabClick = {},
            onRetryClick = {},
            onHomeClick = {},
            onInviteCodeClick = {},
            onNewMapClick = {},
            onInstagramImportClick = {},
            onMapShareImportClick = {},
            onMapClick = {},
        )
    }
}

/** 프라이빗 탭에 나만의 지도만 있을 때: 「프라이빗 지도」 제목과 「편집」은 남고 비었다고 알린다. */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun CollectionScreenPrivateEmptyPreview() {
    MoaMapTheme {
        CollectionContent(
            uiState = CollectionUiState(
                selectedTab = MapType.Private,
                private = MyMapsState.Success(
                    listOf(MyMap(1L, "나만의 지도", null, 1, 3, official = false, personal = true)),
                ),
            ),
            onTabClick = {},
            onRetryClick = {},
            onHomeClick = {},
            onInviteCodeClick = {},
            onNewMapClick = {},
            onInstagramImportClick = {},
            onMapShareImportClick = {},
            onMapClick = {},
        )
    }
}
