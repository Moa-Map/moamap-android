package com.moamap.app.feature.officialmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.MapDetailTopBar
import com.moamap.app.feature.mapdetail.MapLeaveDialog
import com.moamap.app.feature.mapdetail.domain.model.MapDetailAction
import com.moamap.app.feature.mapdetail.presentation.MapDetailViewModel

/**
 * 장소 대신 전용 지도를 보여 주는 공식지도(유동인구·공중화장실)의 틀. 상단바 아래에 [content] 를 둔다.
 *
 * 참여·나가기는 지도 상세와 같다. 같은 지도 번호로 지도 상세의 [MapDetailViewModel] 을 그대로
 * 쓰고, 상단바·나가기 팝업도 지도 상세 것을 쓴다 - 공식지도는 메뉴 없이 참여하기/나가기 글자다.
 *
 * @param initialTitle 서버 이름이 오기 전까지 상단바를 채우는 값.
 */
@Composable
internal fun OfficialMapScaffold(
    initialTitle: String,
    onBackClick: () -> Unit,
    membershipViewModel: MapDetailViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val membership by membershipViewModel.uiState.collectAsStateWithLifecycle()
    // 나가기는 바로 하지 않고 이 팝업에서 한 번 더 묻는다.
    var leaveDialogVisible by rememberSaveable { mutableStateOf(false) }
    val title = membership.title ?: initialTitle

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundSecondary)
                .statusBarsPadding(),
        ) {
            MapDetailTopBar(
                mapTitle = title,
                roleBadge = membership.roleBadge,
                action = membership.action,
                actionEnabled = !membership.actionInProgress,
                onBackClick = onBackClick,
                onActionClick = {
                    if (membership.action == MapDetailAction.Join) {
                        membershipViewModel.join()
                    } else {
                        leaveDialogVisible = true
                    }
                },
            )

            content()
        }

        ErrorSnackbar(
            message = membership.errorMessage,
            onShown = membershipViewModel::consumeErrorMessage,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // 나갈 수 없는 상태가 되면(나가기를 마쳐 참여가 풀리면) 같이 닫힌다.
    val leaveOutcome = membership.leaveOutcome
    if (leaveDialogVisible && leaveOutcome != null) {
        MapLeaveDialog(
            mapName = title,
            outcome = leaveOutcome,
            onConfirm = {
                leaveDialogVisible = false
                membershipViewModel.leave()
            },
            onDismiss = { leaveDialogVisible = false },
        )
    }
}
