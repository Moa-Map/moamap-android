package com.moamap.app.feature.collection

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.component.MoaMapLargeButton
import com.moamap.app.core.designsystem.component.MoaMapNoticeDialog
import com.moamap.app.core.designsystem.component.MoaMapTabTopBarHeight
import com.moamap.app.core.designsystem.component.MoaMapTopBarLogo
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.presentation.CollectionEditState
import com.moamap.app.feature.collection.presentation.CollectionUiState
import com.moamap.app.feature.collection.presentation.CollectionViewModel
import com.moamap.app.feature.collection.presentation.JoinMapDialog
import com.moamap.app.feature.collection.presentation.JoinState
import com.moamap.app.feature.collection.presentation.LeaveEligibility
import com.moamap.app.feature.collection.presentation.MyMapsState
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import kotlinx.coroutines.delay

/** 「지도 나가기」 버튼과 홈 인디케이터 사이. 새 지도 만들기 버튼과 같다. */
private val LeaveButtonBottomPadding = 13.dp

/** 방장 안내 팝업이 저절로 닫히기까지. iOS 와 같다. */
private const val OWNER_NOTICE_MILLIS = 2_500L

private val CollectionTabs = listOf(MapType.Community, MapType.Private)

private val MapType.label: String
    get() = when (this) {
        MapType.Community -> "커뮤니티"
        MapType.Private -> "프라이빗"
        MapType.Official -> "공식"
    }

/**
 * 골라서 나가기 확인 문구. 지금 보고 있는 탭에서 사라진다고 알리고, 프라이빗은 다시 들어오려면 초대 코드가
 * 필요하다고 덧붙인다(피그마 릴리즈 v1 「팝업창」 `5068:52159`).
 */
internal fun collectionLeaveMessage(tab: MapType): String {
    val message = "나가면 ${tab.label} 탭에서 지도가 사라집니다"
    return if (tab == MapType.Private) "$message\n다시 참여하려면 초대 코드가 필요해요" else message
}

@Composable
fun CollectionScreen(
    onHomeClick: () -> Unit = {},
    onNewMapClick: () -> Unit = {},
    onInstagramImportClick: () -> Unit = {},
    onMapShareImportClick: () -> Unit = {},
    onMapClick: (MyMap) -> Unit = {},
    /** 편집 중이라 아래 「지도 나가기」 버튼이 떴는지. 뜬 동안 하단 탭을 숨겨야 한다 - 탭은 NavHost 가 그린다. */
    onSelectionBarVisibleChange: (Boolean) -> Unit = {},
    /** 다른 화면이 돌려준 안내. 받았으면 [onPendingNoticeConsumed] 로 비운다. */
    pendingNotice: String? = null,
    onPendingNoticeConsumed: () -> Unit = {},
    /** 완료 토스트. 하단 탭 위에 그려야 해서 NavHost 가 띄운다. */
    onToast: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(pendingNotice) {
        pendingNotice ?: return@LaunchedEffect
        viewModel.showNotice(pendingNotice)
        onPendingNoticeConsumed()
    }
    LaunchedEffect(uiState.toast) {
        val toast = uiState.toast ?: return@LaunchedEffect
        onToast(toast)
        viewModel.consumeToast()
    }
    val edit = uiState.edit

    // 편집하는 동안은 고르기 전부터 「지도 나가기」 버튼이 하단 탭 자리를 대신한다(시안 「편집 - default」).
    val selectionBarVisible = edit != null
    LaunchedEffect(selectionBarVisible) { onSelectionBarVisibleChange(selectionBarVisible) }
    DisposableEffect(Unit) { onDispose { onSelectionBarVisibleChange(false) } }

    // 편집 중이면 기기 뒤로가기는 편집부터 끝낸다.
    BackHandler(enabled = edit != null) { viewModel.finishEdit() }

    if (edit?.confirmVisible == true) {
        MoaMapConfirmDialog(
            title = "지도에서 나갈까요?",
            message = collectionLeaveMessage(uiState.selectedTab),
            confirmText = "나가기",
            onConfirm = viewModel::leaveSelected,
            onDismissRequest = viewModel::closeLeaveConfirm,
            dismissText = "닫기",
            dismissColor = MoaMapPrimitiveColors.Gray200,
            confirmColor = MoaMapTheme.colors.statusAlert,
        )
    }
    if (edit?.ownerNoticeVisible == true) {
        LaunchedEffect(Unit) {
            delay(OWNER_NOTICE_MILLIS)
            viewModel.closeOwnerNotice()
        }
        MoaMapNoticeDialog(
            title = "방장은 지도를 나갈 수 없습니다.",
            message = "방장인 지도를 빼고 다시 골라주세요",
            onDismissRequest = viewModel::closeOwnerNotice,
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
                    // 시안 「모음/커뮤니티」: 상단 바 아래 12 에 탭.
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
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

        // 안내는 하단 탭(또는 「지도 나가기」 버튼) 위에 쌓는다.
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            ErrorSnackbar(message = uiState.notice, onShown = onNoticeShown)
            if (edit != null) {
                // 하단 탭 자리를 대신한다. 고르기 전에는 회색으로 막힌다(시안 「편집 - default」·「편집 - active」).
                MoaMapLargeButton(
                    label = "지도 나가기",
                    enabled = edit.selected.isNotEmpty() && !edit.leaving,
                    submitting = edit.leaving,
                    onClick = onLeaveClick,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
                        .padding(top = LeaveButtonBottomPadding, bottom = LeaveButtonBottomPadding),
                )
            } else {
                // 하단 탭 자리. 탭은 NavHost 가 그린다.
                Spacer(Modifier.navigationBarsPadding().height(80.dp))
            }
        }
    }
}

/**
 * 모음 상단 바. 시안 「모음탭_앱바」: 높이 58, 「초대 코드」(안쪽 8·열쇠↔글 4) ↔ 「새 지도」 12.
 *
 * 시안은 로고를 홈보다 2 왼쪽(x18)에 두었지만, 탭을 오갈 때 로고가 움찔하지 않게 홈과 같은 자리에 둔다.
 */
@Composable
private fun CollectionTopBar(
    onHomeClick: () -> Unit,
    onInviteCodeClick: () -> Unit,
    onNewMapClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MoaMapTabTopBarHeight)
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
                modifier = Modifier
                    .clickable(onClick = onInviteCodeClick)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun CollectionScreenPreview() {
    MoaMapTheme {
        CollectionContent(
            uiState = CollectionUiState(
                community = MyMapsState.Success(
                    listOf(
                        MyMap(1L, "서울 팝업스토어 맵", null, 2312, 116, official = false, personal = false, role = MapRole.Admin),
                        MyMap(2L, "성수 카페 투어", null, 24, 0, official = false, personal = false, role = MapRole.Owner),
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
                        MyMap(2L, "성수 카페 투어", null, 24, 0, official = false, personal = false, role = MapRole.Owner),
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
