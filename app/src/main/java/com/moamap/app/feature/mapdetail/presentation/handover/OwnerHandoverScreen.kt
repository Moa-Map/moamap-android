package com.moamap.app.feature.mapdetail.presentation.handover

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.common.format.formatMemberCount
import com.moamap.app.core.common.format.formatPlaceCount
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaMapCenteredNotice
import com.moamap.app.core.designsystem.component.MoaMapErrorNotice
import com.moamap.app.core.designsystem.component.MoaMapLargeButton
import com.moamap.app.core.designsystem.component.MoaMapLoadingIndicator
import com.moamap.app.core.designsystem.component.MoaMapTitleTopBar
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.collection.CollectionMapCard
import com.moamap.app.feature.collection.CollectionMapUiModel
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import com.moamap.app.feature.mapdetail.presentation.members.MemberCard
import com.moamap.app.feature.mapdetail.presentation.members.MemberRole
import com.moamap.app.feature.mapdetail.presentation.members.MemberUiModel

/** 「방장 위임하기」와 홈 인디케이터 사이. 모음 편집의 「지도 나가기」와 같다. */
private val SubmitButtonBottomPadding = 13.dp

@Composable
internal fun OwnerHandoverScreen(
    onBackClick: () -> Unit,
    /** 방장을 넘기고 지도에서 나갔다. 연 쪽이 이 화면을 닫고 뒤처리를 한다. */
    onHandedOver: () -> Unit,
    viewModel: OwnerHandoverViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.handedOver) {
        if (uiState.handedOver) onHandedOver()
    }
    // 넘기는 중에는 나가지 못하게 한다. 응답 전에 닫히면 넘어갔는지 모른 채 화면을 떠난다.
    BackHandler(enabled = uiState.submitting) {}

    OwnerHandoverContent(
        uiState = uiState,
        onBackClick = { if (!uiState.submitting) onBackClick() },
        onMemberClick = viewModel::select,
        onSubmitClick = viewModel::handOver,
        onRetryClick = viewModel::retry,
        onNoticeShown = viewModel::consumeNotice,
    )
}

@Composable
private fun OwnerHandoverContent(
    uiState: OwnerHandoverUiState,
    onBackClick: () -> Unit,
    onMemberClick: (Long) -> Unit,
    onSubmitClick: () -> Unit,
    onRetryClick: () -> Unit,
    onNoticeShown: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundSecondary)
                .statusBarsPadding(),
        ) {
            MoaMapTitleTopBar(title = "방장 위임", onBackClick = onBackClick)
            LazyColumn(
                modifier = Modifier.weight(1f),
                // 시안: 상단 바 아래 8 에서 지도 카드가 시작한다.
                contentPadding = PaddingValues(
                    start = MoaMapDimens.ScreenHorizontalPadding,
                    top = 8.dp,
                    end = MoaMapDimens.ScreenHorizontalPadding,
                    bottom = 24.dp,
                ),
                // 시안: 사용자 카드 사이 4. 지도 카드·제목과의 사이 20 은 그 항목에서 더한다.
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                uiState.map?.let { map ->
                    item(key = "map") {
                        CollectionMapCard(
                            map = CollectionMapUiModel(
                                id = map.id,
                                title = map.title,
                                imageUrl = map.imageUrl,
                                placeCount = formatPlaceCount(map.placeCount),
                                memberCount = formatMemberCount(map.memberCount),
                                role = MapRole.Owner,
                            ),
                            onClick = {},
                            modifier = Modifier.padding(bottom = 16.dp),
                            border = BorderStroke(1.dp, MoaMapPrimitiveColors.Yellow500),
                            color = MoaMapPrimitiveColors.Yellow50,
                            // 손잡이 없는 16 여백 카드다.
                            reorderable = true,
                        )
                    }
                }
                item(key = "header") {
                    HandoverHeader(toAdmins = uiState.toAdmins, modifier = Modifier.padding(bottom = 16.dp))
                }
                when {
                    uiState.loading -> item(key = "loading") { MoaMapCenteredNotice { MoaMapLoadingIndicator() } }

                    uiState.errorMessage != null -> item(key = "error") {
                        MoaMapCenteredNotice {
                            MoaMapErrorNotice(message = uiState.errorMessage, onRetryClick = onRetryClick)
                        }
                    }

                    uiState.candidates.isEmpty() -> item(key = "empty") {
                        MoaMapCenteredNotice {
                            Text(
                                text = HANDOVER_ALONE_MESSAGE,
                                style = MoaMapTheme.typography.body2,
                                color = MoaMapTheme.colors.textAssistive,
                            )
                        }
                    }

                    else -> items(uiState.candidates, key = { member -> member.id }) { member ->
                        MemberCard(
                            member = member,
                            // 관리자만 후보일 때 노란 왕관이 붙는다(시안 `5104:58364`). 일반 멤버는 없다.
                            tag = member.role.takeIf { role -> role == MemberRole.Admin },
                            canGrant = false,
                            grantEnabled = false,
                            onGrantRoleClick = {},
                            selected = member.id == uiState.selectedId,
                            onClick = { onMemberClick(member.id) },
                        )
                    }
                }
            }
            MoaMapLargeButton(
                label = "방장 위임하기",
                onClick = onSubmitClick,
                enabled = uiState.canSubmit,
                submitting = uiState.submitting,
                modifier = Modifier
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding)
                    .navigationBarsPadding()
                    .padding(bottom = SubmitButtonBottomPadding),
            )
        }

        ErrorSnackbar(
            message = uiState.notice,
            onShown = onNoticeShown,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun HandoverHeader(toAdmins: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "새로운 방장을 선택해주세요",
            style = MoaMapTheme.typography.subtitle1.withDesignLineHeight(),
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "${if (toAdmins) "관리자" else "멤버"} 중 한 명에게 방장을 위임할 수 있어요",
            style = MoaMapTheme.typography.body2.withDesignLineHeight(),
            color = MoaMapTheme.colors.textAssistive,
        )
    }
}

/** 프리뷰 전용 표본. 지어낸 이름이라 화면에 넘기지 않는다. */
private val PreviewCandidates = listOf(
    MemberUiModel(2L, "이서연", null, MemberRole.Admin, placeCount = 5),
    MemberUiModel(3L, "박지훈", null, MemberRole.Admin, placeCount = 0),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun OwnerHandoverContentPreview() {
    MoaMapTheme {
        OwnerHandoverContent(
            uiState = OwnerHandoverUiState(loading = false, candidates = PreviewCandidates, selectedId = 2L),
            onBackClick = {},
            onMemberClick = {},
            onSubmitClick = {},
            onRetryClick = {},
            onNoticeShown = {},
        )
    }
}
