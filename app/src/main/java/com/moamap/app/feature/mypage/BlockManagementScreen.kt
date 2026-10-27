package com.moamap.app.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapLoadingIndicator
import com.moamap.app.core.designsystem.component.MoaMapSymbolNotice
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.component.MoaMapToast
import com.moamap.app.core.designsystem.component.PhotoThumbnail
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.mypage.domain.model.BlockedUser
import com.moamap.app.feature.mypage.presentation.BlockManagementUiState
import com.moamap.app.feature.mypage.presentation.BlockManagementViewModel

private val BlockedUserCardShape = RoundedCornerShape(12.dp)
private val AvatarSize = 50.dp
private val UnblockButtonShape = RoundedCornerShape(1000.dp)

/**
 * 설정 「차단 관리」. 시안 「차단 관리 - 차단 멤버 목록」(`5159:32910`)·「차단 멤버 X」(`5159:33244`)·
 * 「차단 해제 팝업」(`5159:33136`).
 *
 * 카드는 멤버 관리와 같은 모양이지만 「등록한 장소 수」 줄을 빼고 이름만 가운데에 둔다(이슈 #392 사용자 결정).
 */
@Composable
internal fun BlockManagementScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BlockManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BlockManagementContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = viewModel::retry,
        onUnblockConfirm = viewModel::unblock,
        onErrorShown = viewModel::consumeError,
        onToastShown = viewModel::consumeToast,
        modifier = modifier,
    )
}

@Composable
private fun BlockManagementContent(
    uiState: BlockManagementUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onUnblockConfirm: (Long) -> Unit,
    onErrorShown: () -> Unit,
    onToastShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 화면을 돌려도 팝업이 남도록 사람 id 만 저장한다.
    var confirmUserId by rememberSaveable { mutableStateOf<Long?>(null) }
    val confirmUser = uiState.users.firstOrNull { user -> user.userId == confirmUserId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            MoaMapTitleTopBar(title = "차단 관리", onBackClick = onBackClick)
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.loading -> MoaMapLoadingIndicator(modifier = Modifier.align(Alignment.Center))

                    uiState.loadErrorMessage != null -> MoaMapErrorNotice(
                        message = uiState.loadErrorMessage,
                        onRetryClick = onRetryClick,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    uiState.empty -> Unit

                    else -> BlockedUserList(
                        users = uiState.users,
                        unblocking = uiState.unblockingUserId != null,
                        onUnblockClick = { user -> confirmUserId = user.userId },
                    )
                }
            }
        }

        // 시안은 상단 바 아래 공간이 아니라 화면 전체의 가운데에 둔다.
        if (uiState.empty) {
            MoaMapSymbolNotice(
                title = "차단한 사용자가 없어요",
                titleColor = MoaMapTheme.colors.textAlternative,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = "차단한 사용자는 여기에 표시돼요",
                    style = MoaMapTheme.typography.body2,
                    color = MoaMapTheme.colors.textAssistive,
                    textAlign = TextAlign.Center,
                )
            }
        }

        ErrorSnackbar(
            message = uiState.errorMessage,
            onShown = onErrorShown,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
        // 시안에 이 화면의 자리가 없어 지도 정보 화면과 같은 높이(시스템 막대 위 32)에 둔다.
        MoaMapToast(
            message = uiState.toastMessage,
            onShown = onToastShown,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        )
    }

    if (confirmUser != null) {
        MoaMapConfirmDialog(
            title = "${confirmUser.nickname}님의 차단을 해제할까요?",
            message = "이 사용자의 댓글과 게시물이\n다시 보이게 돼요",
            confirmText = "해제하기",
            onConfirm = {
                confirmUserId = null
                onUnblockConfirm(confirmUser.userId)
            },
            onDismissRequest = { confirmUserId = null },
            dismissText = "취소",
            dismissColor = MoaMapPrimitiveColors.Gray200,
        )
    }
}

@Composable
private fun BlockedUserList(
    users: List<BlockedUser>,
    unblocking: Boolean,
    onUnblockClick: (BlockedUser) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MoaMapDimens.ScreenHorizontalPadding,
            end = MoaMapDimens.ScreenHorizontalPadding,
            top = 20.dp,
            bottom = 20.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "header") {
            BlockedUsersHeader(modifier = Modifier.padding(bottom = 16.dp))
        }
        items(users, key = { user -> user.userId }) { user ->
            BlockedUserCard(user = user, enabled = !unblocking, onUnblockClick = { onUnblockClick(user) })
        }
    }
}

/** 「차단한 사용자」 제목과 ⓘ 안내. 세로로 쌓는 글자라 줄 높이를 살린다. */
@Composable
private fun BlockedUsersHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "차단한 사용자",
            style = MoaMapTheme.typography.subtitle1.withDesignLineHeight(),
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.semantics { heading() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = MoaMapTheme.colors.textAssistive,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "차단한 사용자의 댓글과 게시물은 보이지 않아요",
                style = MoaMapTheme.typography.body2.withDesignLineHeight(),
                color = MoaMapTheme.colors.textAssistive,
            )
        }
    }
}

/** 시안 「사용자 리스트」: 흰 카드, 프로필 50 원형, 이름, 오른쪽 회색 알약 [차단 해제]. */
@Composable
private fun BlockedUserCard(
    user: BlockedUser,
    enabled: Boolean,
    onUnblockClick: () -> Unit,
) {
    ShadowedSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = BlockedUserCardShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhotoThumbnail(imageUrl = user.profileImageUrl, size = AvatarSize, shape = CircleShape)
                Text(
                    text = user.nickname,
                    style = MoaMapTheme.typography.subtitle2,
                    color = MoaMapTheme.colors.textNormal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .clip(UnblockButtonShape)
                    .background(MoaMapPrimitiveColors.Gray200)
                    .clickable(enabled = enabled, onClick = onUnblockClick)
                    .padding(8.dp),
            ) {
                Text(
                    text = "차단 해제",
                    style = MoaMapTheme.typography.button2,
                    color = MoaMapTheme.colors.textWhite,
                )
            }
        }
    }
}

/** 프리뷰 전용 표본. 화면에 넘기지 않는다. */
private val PreviewBlockedUsers = listOf(
    BlockedUser(1L, "김도현", null),
    BlockedUser(2L, "이서연", null),
    BlockedUser(3L, "박지훈", null),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun BlockManagementPreview() {
    MoaMapTheme {
        BlockManagementContent(
            uiState = BlockManagementUiState(loading = false, users = PreviewBlockedUsers),
            onBackClick = {},
            onRetryClick = {},
            onUnblockConfirm = {},
            onErrorShown = {},
            onToastShown = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun BlockManagementEmptyPreview() {
    MoaMapTheme {
        BlockManagementContent(
            uiState = BlockManagementUiState(loading = false),
            onBackClick = {},
            onRetryClick = {},
            onUnblockConfirm = {},
            onErrorShown = {},
            onToastShown = {},
        )
    }
}
