package com.moamap.app.feature.mypage.presentation

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.UNBLOCKED_TOAST
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.feature.mypage.domain.model.BlockedUser
import com.moamap.app.feature.mypage.domain.repository.BlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BlockManagementVM"

internal const val BLOCKED_USERS_LOAD_FAILED_MESSAGE = "차단한 사용자를 불러오지 못했어요"
internal const val UNBLOCK_FAILED_MESSAGE = "차단을 해제하지 못했어요"

/**
 * 차단 관리 상태.
 *
 * 목록을 못 받은 것([loadErrorMessage], 화면 가운데 다시 시도)과 해제에 실패한 것([errorMessage], 아래 안내)을 나눈다.
 */
@Immutable
data class BlockManagementUiState(
    val loading: Boolean = true,
    val users: List<BlockedUser> = emptyList(),
    val loadErrorMessage: String? = null,
    /** 해제 요청이 나가 있는 사람. 그동안 [차단 해제]를 또 누르지 못하게 한다. */
    val unblockingUserId: Long? = null,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
) {
    val empty: Boolean get() = !loading && loadErrorMessage == null && users.isEmpty()
}

@HiltViewModel
class BlockManagementViewModel @Inject constructor(
    private val repository: BlockRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockManagementUiState())
    val uiState: StateFlow<BlockManagementUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    /** 해제되면 목록에서 빼고 토스트를 띄운다. 실패하면 목록은 그대로 두고 안내만 띄운다. */
    fun unblock(userId: Long) {
        if (_uiState.value.unblockingUserId != null) return
        _uiState.update { state -> state.copy(unblockingUserId = userId, errorMessage = null) }

        viewModelScope.launch {
            try {
                repository.unblock(userId)
                _uiState.update { state ->
                    state.copy(
                        users = state.users.filterNot { user -> user.userId == userId },
                        unblockingUserId = null,
                        toastMessage = UNBLOCKED_TOAST,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "차단 해제 실패 (userId=$userId)", e)
                _uiState.update { state ->
                    state.copy(unblockingUserId = null, errorMessage = e.toUserMessage(UNBLOCK_FAILED_MESSAGE))
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    fun consumeToast() {
        _uiState.update { state -> state.copy(toastMessage = null) }
    }

    private fun load() {
        _uiState.update { state -> state.copy(loading = true, loadErrorMessage = null) }
        viewModelScope.launch {
            try {
                val users = repository.getBlockedUsers()
                _uiState.update { state -> state.copy(loading = false, users = users) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "차단 목록 조회 실패", e)
                _uiState.update { state ->
                    state.copy(loading = false, loadErrorMessage = e.toUserMessage(BLOCKED_USERS_LOAD_FAILED_MESSAGE))
                }
            }
        }
    }
}
