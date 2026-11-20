package com.moamap.app.feature.mapdetail.presentation.handover

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.component.MoaMapNoticeDialog
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import com.moamap.app.feature.mapdetail.domain.repository.MapMemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "OwnerHandoverPrompt"

internal const val HANDOVER_CHECK_FAILED_MESSAGE = "멤버 목록을 불러오지 못했어요"

/** 넘길 사람이 없을 때. 혼자 남은 커뮤니티 지도의 방장이다. */
internal const val HANDOVER_ALONE_MESSAGE = "함께하는 멤버가 없어 방장을 넘길 수 없어요"

/** 혼자 남은 안내가 저절로 닫히기까지. 다른 방장 안내 팝업과 같다. */
private const val ALONE_NOTICE_MILLIS = 2_500L

/** [나가기]를 누른 방장에게 띄울 안내. */
internal sealed interface HandoverPrompt {
    /** 위임 안내(시안 `5154:27665`). [toAdmins] 면 관리자에게, 아니면 멤버에게 넘기라고 적는다. */
    data class Ask(val mapId: Long, val toAdmins: Boolean) : HandoverPrompt

    /** 넘길 사람이 없다. 상세를 읽은 뒤 다른 멤버가 모두 나갔을 때다. */
    data object Alone : HandoverPrompt
}

@Immutable
internal data class HandoverPromptUiState(
    val prompt: HandoverPrompt? = null,
    /** 후보를 묻는 중. 연달아 눌러도 한 번만 묻는다. */
    val checking: Boolean = false,
    /** 한 번 보여주고 지우는 실패 안내. 띄우는 자리는 화면마다 다르다. */
    val errorMessage: String? = null,
)

/** 관리자가 있으면 관리자 중에서만 고를 수 있다(서버 규칙). 안내 문구도 그에 맞춘다(11-20 사용자 결정). */
internal fun handoverPromptMessage(toAdmins: Boolean): String =
    "다른 회원이 지도를 계속 이용할 수 있도록\n${if (toAdmins) "관리자" else "멤버"}에게 방장을 넘겨주세요"

/**
 * 방장이 [나가기]를 누르면 넘길 사람이 있는지 먼저 묻는다. 모음 편집과 지도 상세가 함께 쓴다.
 *
 * 안내 문구가 관리자 유무에 따라 달라서 팝업을 띄우기 전에 후보를 받는다. 방장 위임 화면은 들어가서 다시
 * 받는다 - 그사이 누가 나갔을 수 있다.
 */
@HiltViewModel
internal class OwnerHandoverPromptViewModel @Inject constructor(
    private val repository: MapMemberRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HandoverPromptUiState())
    val uiState: StateFlow<HandoverPromptUiState> = _uiState.asStateFlow()

    fun open(mapId: Long) {
        if (_uiState.value.checking) return
        _uiState.update { state -> state.copy(checking = true, prompt = null, errorMessage = null) }

        viewModelScope.launch {
            try {
                val candidates = repository.getOwnerCandidates(mapId)
                val prompt = if (candidates.isEmpty()) {
                    HandoverPrompt.Alone
                } else {
                    HandoverPrompt.Ask(mapId, toAdmins = candidates.any { member -> member.role == MapRole.Admin })
                }
                _uiState.update { state -> state.copy(checking = false, prompt = prompt) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "방장 후보 조회 실패 (mapId=$mapId)", e)
                _uiState.update { state ->
                    state.copy(checking = false, errorMessage = e.toUserMessage(HANDOVER_CHECK_FAILED_MESSAGE))
                }
            }
        }
    }

    fun dismiss() {
        _uiState.update { state -> state.copy(prompt = null) }
    }

    fun consumeError() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }
}

/**
 * [prompt] 에 맞는 팝업. [멤버 선택]을 누르면 팝업을 닫고 [onSelectMemberClick] 으로 방장 위임 화면에 간다.
 */
@Composable
internal fun OwnerHandoverPromptDialog(
    prompt: HandoverPrompt?,
    onSelectMemberClick: (mapId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    when (prompt) {
        null -> Unit

        is HandoverPrompt.Ask -> MoaMapConfirmDialog(
            title = "나가기 전에 방장을 위임해 주세요",
            message = handoverPromptMessage(prompt.toAdmins),
            confirmText = "멤버 선택",
            onConfirm = {
                onDismiss()
                onSelectMemberClick(prompt.mapId)
            },
            onDismissRequest = onDismiss,
            dismissText = "취소",
            dismissColor = MoaMapPrimitiveColors.Gray200,
        )

        HandoverPrompt.Alone -> {
            LaunchedEffect(Unit) {
                delay(ALONE_NOTICE_MILLIS)
                onDismiss()
            }
            MoaMapNoticeDialog(
                title = "방장은 지도를 나갈 수 없습니다.",
                message = HANDOVER_ALONE_MESSAGE,
                onDismissRequest = onDismiss,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun OwnerHandoverPromptDialogPreview() {
    MoaMapTheme {
        OwnerHandoverPromptDialog(
            prompt = HandoverPrompt.Ask(mapId = 1L, toAdmins = true),
            onSelectMemberClick = {},
            onDismiss = {},
        )
    }
}
