package com.example.moamap.feature.onboarding.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.core.network.ApiException
import com.example.moamap.core.network.ConnectionException
import com.example.moamap.feature.onboarding.domain.model.KakaoLoginCancelledException
import com.example.moamap.feature.onboarding.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * [context] 는 카카오 SDK 가 로그인 화면을 띄우는 데 필요한 Activity 컨텍스트다.
     * 보관하지 않고 이 호출 동안에만 넘긴다.
     */
    fun loginWithKakao(context: Context) {
        // 버튼 연타로 로그인 창이 여러 번 뜨지 않게 한다.
        if (_uiState.value == LoginUiState.Loading) return

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                authRepository.loginWithKakao(context)
                _uiState.value = LoginUiState.Success
            } catch (e: CancellationException) {
                throw e
            } catch (_: KakaoLoginCancelledException) {
                // 실패가 아니라 사용자의 선택이므로 조용히 원래 상태로 돌아간다.
                _uiState.value = LoginUiState.Idle
            } catch (throwable: Throwable) {
                // 사용자에게는 짧은 메시지만 보여주므로 원인은 로그에 남긴다.
                Log.e(TAG, "카카오 로그인 실패", throwable)
                _uiState.value = LoginUiState.Error(throwable.toUserMessage())
            }
        }
    }

    /** 이동 신호를 소비한다. 화면 회전 후 같은 상태로 다시 이동하는 것을 막는다. */
    fun consumeSuccess() {
        _uiState.update { state -> if (state == LoginUiState.Success) LoginUiState.Idle else state }
    }

    fun consumeError() {
        _uiState.update { state -> if (state is LoginUiState.Error) LoginUiState.Idle else state }
    }
}

private const val TAG = "LoginViewModel"
private const val DEFAULT_LOGIN_ERROR = "카카오 로그인에 실패했어요"

private fun Throwable.toUserMessage(): String = when (this) {
    is ApiException -> serverMessage.ifBlank { DEFAULT_LOGIN_ERROR }
    is ConnectionException -> "네트워크에 연결할 수 없어요"
    else -> DEFAULT_LOGIN_ERROR
}
