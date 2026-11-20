package com.moamap.app.feature.mapdetail.presentation.handover

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.core.navigation.requireMapId
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.repository.MapDetailRepository
import com.moamap.app.feature.mapdetail.domain.repository.MapMemberRepository
import com.moamap.app.feature.mapdetail.presentation.members.MemberRole
import com.moamap.app.feature.mapdetail.presentation.members.MemberUiModel
import com.moamap.app.feature.mapdetail.presentation.members.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "OwnerHandoverViewModel"

internal const val HANDOVER_LOAD_FAILED_MESSAGE = "멤버 목록을 불러오지 못했어요"
internal const val HANDOVER_FAILED_MESSAGE = "방장을 넘기지 못했어요"
internal const val HANDOVER_NOT_CANDIDATE_MESSAGE = "고른 멤버에게 넘길 수 없어요. 목록을 다시 불러왔어요"

/** `[400] MAP_028`: 후보가 아닌 사람을 골랐다. 그사이 나갔거나 역할이 바뀌었다. */
private const val NOT_CANDIDATE = "MAP_028"

/** `[400] MAP_008`: 혼자 남은 지도의 방장이다. 그사이 다른 멤버가 모두 나갔다. */
private const val ALONE_OWNER = "MAP_008"

@Immutable
internal data class OwnerHandoverUiState(
    /** 위의 지도 카드. 지도를 못 읽어도 위임은 할 수 있어 카드만 숨긴다. */
    val map: MapDetail? = null,
    val candidates: List<MemberUiModel> = emptyList(),
    val loading: Boolean = true,
    /** 후보를 못 읽었다. 본문에 다시 시도와 함께 남는다. */
    val errorMessage: String? = null,
    val selectedId: Long? = null,
    /** 넘기는 중. 버튼을 연달아 눌러도 한 번만 간다. */
    val submitting: Boolean = false,
    /** 한 번 보여주고 지우는 실패 안내. 목록은 그대로 둔다. */
    val notice: String? = null,
    /** 넘기고 나갔다. 화면이 이 신호를 보고 닫힌다. */
    val handedOver: Boolean = false,
) {
    /** 관리자가 있으면 관리자만 후보로 온다(서버 규칙). 안내 문구가 이걸 보고 고른다. */
    val toAdmins: Boolean get() = candidates.any { member -> member.role == MemberRole.Admin }

    val canSubmit: Boolean
        get() = !submitting && candidates.any { member -> member.id == selectedId }
}

/**
 * 방장 위임(시안 `5154:26719`·`5104:58364`). 새 방장을 한 명 고르고, 넘기기와 나가기를 한 번에 한다.
 *
 * 모음 편집과 지도 상세 어디서 들어와도 같다. 끝나면 [OwnerHandoverUiState.handedOver] 로 알리고, 이 화면을
 * 연 쪽이 뒤처리를 한다.
 */
@HiltViewModel
internal class OwnerHandoverViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: MapMemberRepository,
    private val mapRepository: MapDetailRepository,
) : ViewModel() {

    private val mapId: Long = savedStateHandle.requireMapId()

    private val _uiState = MutableStateFlow(OwnerHandoverUiState())
    val uiState: StateFlow<OwnerHandoverUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    fun select(userId: Long) {
        if (_uiState.value.submitting) return
        _uiState.update { state -> state.copy(selectedId = userId) }
    }

    fun handOver() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val newOwnerId = state.selectedId ?: return

        _uiState.update { current -> current.copy(submitting = true, notice = null) }
        viewModelScope.launch {
            try {
                memberRepository.handOverAndLeave(mapId = mapId, newOwnerId = newOwnerId)
                _uiState.update { current -> current.copy(submitting = false, handedOver = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 사용자 식별자는 남기지 않는다. 로그가 수집·보관되는 경로를 타기 때문이다.
                Log.w(TAG, "방장 위임 실패 (mapId=$mapId)", e)
                val code = (e as? ApiException)?.code
                _uiState.update { current ->
                    current.copy(
                        submitting = false,
                        notice = when (code) {
                            NOT_CANDIDATE -> HANDOVER_NOT_CANDIDATE_MESSAGE
                            ALONE_OWNER -> HANDOVER_ALONE_MESSAGE
                            else -> e.toUserMessage(HANDOVER_FAILED_MESSAGE)
                        },
                    )
                }
                // 후보가 바뀐 것이라 목록을 새로 받는다. 고른 사람이 빠졌으면 고르기도 풀린다.
                if (code == NOT_CANDIDATE || code == ALONE_OWNER) loadCandidates()
            }
        }
    }

    fun consumeNotice() {
        _uiState.update { state -> state.copy(notice = null) }
    }

    private fun load() {
        _uiState.update { state -> state.copy(loading = true, errorMessage = null) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val map = async { loadMap() }
            loadCandidates()
            val detail = map.await()
            _uiState.update { state -> state.copy(map = detail ?: state.map) }
        }
    }

    private suspend fun loadCandidates() {
        try {
            val candidates = memberRepository.getOwnerCandidates(mapId).map { member -> member.toUiModel() }
            _uiState.update { state ->
                state.copy(
                    loading = false,
                    candidates = candidates,
                    errorMessage = null,
                    selectedId = state.selectedId?.takeIf { id -> candidates.any { member -> member.id == id } },
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "방장 후보 조회 실패 (mapId=$mapId)", e)
            _uiState.update { state ->
                state.copy(loading = false, errorMessage = e.toUserMessage(HANDOVER_LOAD_FAILED_MESSAGE))
            }
        }
    }

    /** 실패하면 null. 카드만 빠지고 위임은 계속할 수 있다. */
    private suspend fun loadMap(): MapDetail? = try {
        mapRepository.getMapDetail(mapId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "지도 조회 실패 (mapId=$mapId)", e)
        null
    }
}
