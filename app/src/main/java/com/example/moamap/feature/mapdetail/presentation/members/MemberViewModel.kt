package com.example.moamap.feature.mapdetail.presentation.members

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.feature.mapdetail.domain.repository.MapMemberRepository
import com.example.moamap.feature.mapdetail.presentation.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MemberViewModel"

internal const val MEMBER_LOAD_FAILED_MESSAGE = "멤버 목록을 불러오지 못했어요"

internal const val GRANT_ROLE_FAILED_MESSAGE = "권한을 주지 못했어요"

/**
 * 멤버 관리 시트 상태.
 *
 * 목록을 못 읽은 것([errorMessage])과 권한 부여가 실패한 것([grantErrorMessage])을 나눈다.
 * 앞은 시트 본문에 재시도와 함께 남고, 뒤는 목록을 그대로 둔 채 스낵바로 한 번 스쳐 간다.
 * 하나로 묶으면 권한 한 번 잘못 준 것 때문에 이미 읽어 둔 목록이 사라진다.
 */
@Immutable
internal data class MemberUiState(
    val loading: Boolean = true,
    val members: List<MemberUiModel> = emptyList(),
    val errorMessage: String? = null,
    /** 권한 부여가 오가는 중. 버튼을 연달아 눌러도 서버에는 한 번만 간다. */
    val granting: Boolean = false,
    val grantErrorMessage: String? = null,
)

/**
 * 지도 멤버 관리.
 *
 * [com.example.moamap.feature.mapdetail.presentation.MapDetailViewModel] 에 얹지 않는다.
 * 시트를 열 때만 필요한 값이라, 지도 상태에 섞으면 장소 탭만 보는 사용자도 화면에 들어올
 * 때마다 멤버를 함께 받게 된다.
 */
@HiltViewModel
internal class MemberViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MapMemberRepository,
) : ViewModel() {

    private val mapId: Long = checkNotNull(savedStateHandle[MoaMapRoute.MapDetail.ARG_MAP_ID]) {
        "지도 상세는 ${MoaMapRoute.MapDetail.ARG_MAP_ID} 없이 열 수 없다"
    }

    private val _uiState = MutableStateFlow(MemberUiState())
    val uiState: StateFlow<MemberUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var grantJob: Job? = null

    /** 한 번이라도 읽기 시작했는가. 시트를 여닫을 때마다 다시 받지 않으려고 본다. */
    private var started = false

    /** 시트가 처음 열렸다. `init` 에서 읽으면 시트를 안 여는 사용자도 멤버를 받게 된다. */
    fun loadOnce() {
        if (started) return
        started = true
        load()
    }

    fun retry() {
        _uiState.update { state -> state.copy(loading = true, errorMessage = null) }
        load()
    }

    /**
     * 일반 멤버를 관리자로 올린다.
     *
     * 성공하면 목록을 다시 받지 않고 그 사람의 역할만 바꾼다. 시트를 다시 그리려고 통신을 한
     * 번 더 하면, 그 조회가 실패했을 때 이미 반영된 권한이 없던 일처럼 보인다.
     */
    fun grantAdmin(userId: Long) {
        if (_uiState.value.granting) return

        _uiState.update { state -> state.copy(granting = true, grantErrorMessage = null) }
        grantJob?.cancel()
        grantJob = viewModelScope.launch {
            try {
                repository.grantAdmin(mapId = mapId, userId = userId)
                _uiState.update { state ->
                    state.copy(
                        granting = false,
                        members = state.members.map { member ->
                            if (member.id == userId) {
                                member.copy(role = MemberRole.Admin)
                            } else {
                                member
                            }
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 사용자 식별자는 남기지 않는다. 로그가 수집·보관되는 경로를 타기 때문이다.
                Log.w(TAG, "권한 부여 실패 (mapId=$mapId)", e)
                _uiState.update { state ->
                    state.copy(
                        granting = false,
                        grantErrorMessage = e.toUserMessage(GRANT_ROLE_FAILED_MESSAGE),
                    )
                }
            }
        }
    }

    /** 스낵바가 한 번 뜨고 나면 지운다. 화면을 되돌아올 때 다시 뜨지 않게 한다. */
    fun consumeGrantError() {
        _uiState.update { state -> state.copy(grantErrorMessage = null) }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val members = repository.getMembers(mapId).map { member -> member.toUiModel() }
                _uiState.update { state ->
                    state.copy(loading = false, members = members, errorMessage = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "멤버 목록 조회 실패 (mapId=$mapId)", e)
                _uiState.update { state ->
                    state.copy(
                        loading = false,
                        errorMessage = e.toUserMessage(MEMBER_LOAD_FAILED_MESSAGE),
                    )
                }
            }
        }
    }
}
