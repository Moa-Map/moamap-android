package com.moamap.app.feature.onboarding.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.NETWORK_ERROR_MESSAGE
import com.moamap.app.core.common.actionableMessage
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.onboarding.domain.model.GoogleLoginCancelledException
import com.moamap.app.feature.onboarding.domain.model.KakaoLoginCancelledException
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.domain.TermsAgreementCheck
import com.moamap.app.feature.terms.domain.checkAgreement
import com.moamap.app.feature.terms.domain.repository.TermsRepository
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
    private val termsRepository: TermsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * [context] 는 카카오 SDK 가 로그인 화면을 띄우는 데 필요한 Activity 컨텍스트다.
     * 보관하지 않고 이 호출 동안에만 넘긴다.
     */
    fun loginWithKakao(context: Context) {
        login(defaultError = DEFAULT_KAKAO_LOGIN_ERROR) { authRepository.loginWithKakao(context) }
    }

    /** [context] 는 Credential Manager 가 계정 선택 화면을 띄우는 데 필요한 Activity 컨텍스트다. */
    fun loginWithGoogle(context: Context) {
        login(defaultError = DEFAULT_GOOGLE_LOGIN_ERROR) { authRepository.loginWithGoogle(context) }
    }

    private fun login(defaultError: String, block: suspend () -> Unit) {
        // 버튼 연타로 로그인 창이 여러 번 뜨지 않게 한다. 다른 소셜 로그인이 진행 중일 때도 막는다.
        if (_uiState.value == LoginUiState.Loading) return

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                if (throwable is KakaoLoginCancelledException || throwable is GoogleLoginCancelledException) {
                    // 실패가 아니라 사용자의 선택이므로 조용히 원래 상태로 돌아간다.
                    _uiState.value = LoginUiState.Idle
                    return@launch
                }
                // 사용자에게는 짧은 메시지만 보여주므로 원인은 로그에 남긴다.
                Log.e(TAG, defaultError, throwable)
                _uiState.value = LoginUiState.Error(throwable.toUserMessage(defaultError))
                return@launch
            }
            checkTermsAgreement(defaultError)
        }
    }

    /** 「다시 시도」. 로그인은 이미 됐으니 약관 동의 상태만 다시 묻는다. */
    fun retryTermsCheck() {
        if (_uiState.value != LoginUiState.TermsCheckFailed) return
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch { checkTermsAgreement(DEFAULT_LOGIN_ERROR) }
    }

    /** 로그인 버튼의 로딩을 이어 가며 동의 상태를 묻고, 결과대로 동의 화면이나 홈으로 보낸다. */
    private suspend fun checkTermsAgreement(defaultError: String) {
        _uiState.value = when (termsRepository.checkAgreement(authRepository)) {
            TermsAgreementCheck.NeedsAgreement -> LoginUiState.Success(needsTermsAgreement = true)
            TermsAgreementCheck.Agreed -> LoginUiState.Success(needsTermsAgreement = false)
            TermsAgreementCheck.Failed -> LoginUiState.TermsCheckFailed
            // 방금 받은 세션을 서버가 끝냈다. 남은 것을 지우고 로그인 화면에 머문다.
            TermsAgreementCheck.SessionExpired -> {
                try {
                    authRepository.logout()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "약관 확인 중 세션 만료 뒤 로그아웃 실패", e)
                }
                LoginUiState.Error(defaultError)
            }
        }
    }

    /** 이동 신호를 소비한다. 화면 회전 후 같은 상태로 다시 이동하는 것을 막는다. */
    fun consumeSuccess() {
        _uiState.update { state -> if (state is LoginUiState.Success) LoginUiState.Idle else state }
    }

    fun consumeError() {
        _uiState.update { state -> if (state is LoginUiState.Error) LoginUiState.Idle else state }
    }
}

private const val TAG = "LoginViewModel"
private const val DEFAULT_KAKAO_LOGIN_ERROR = "카카오 로그인에 실패했어요"
private const val DEFAULT_GOOGLE_LOGIN_ERROR = "구글 로그인에 실패했어요"
private const val DEFAULT_LOGIN_ERROR = "로그인에 실패했어요"

private fun Throwable.toUserMessage(defaultError: String): String = when (this) {
    is ApiException -> actionableMessage() ?: defaultError
    is ConnectionException -> NETWORK_ERROR_MESSAGE
    else -> defaultError
}
