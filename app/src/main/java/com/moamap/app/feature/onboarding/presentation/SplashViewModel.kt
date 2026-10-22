package com.moamap.app.feature.onboarding.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.domain.TermsAgreementCheck
import com.moamap.app.feature.terms.domain.checkAgreement
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SplashVM"

/** 스플래시 다음에 갈 곳. */
enum class SplashDestination {
    /** 아직 판단 중이거나 약관 동의 확인에 실패했다. 스플래시를 계속 보여준다. */
    Undecided,
    Login,
    TermsAgreement,
    Main,
}

/** 로고가 번쩍이고 사라지지 않도록 보장하는 최소 노출 시간. */
private const val MINIMUM_VISIBLE_MILLIS = 1_500L

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val termsRepository: TermsRepository,
) : ViewModel() {

    private val _destination = MutableStateFlow(SplashDestination.Undecided)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    private val _termsCheckFailed = MutableStateFlow(false)

    /** 약관 동의 여부를 확인하지 못했다. 로고 아래에 안내와 「다시 시도」를 띄운다. */
    val termsCheckFailed: StateFlow<Boolean> = _termsCheckFailed.asStateFlow()

    private val _loggingOut = MutableStateFlow(false)

    /** 「로그아웃」을 진행 중. 「다시 시도」·「로그아웃」을 다시 누를 수 없다. */
    val loggingOut: StateFlow<Boolean> = _loggingOut.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            // 판단과 최소 노출 시간을 동시에 돌린다. 판단이 더 오래 걸리면 끝날 때까지 기다린다.
            val destination = async { decideDestination() }
            delay(MINIMUM_VISIBLE_MILLIS)
            show(destination.await())
        }
    }

    /** 「다시 시도」. 동의 상태만 다시 묻는다. */
    fun retry() {
        if (!_termsCheckFailed.value || _loggingOut.value) return
        _termsCheckFailed.value = false
        viewModelScope.launch { show(decideDestination()) }
    }

    /**
     * 확인 실패 안내 아래의 「로그아웃」. 서버가 계속 실패해도 앱 안에서 빠져나갈 수 있게 한다.
     *
     * 실패하면 세션이 남았을 수 있어 화면에 머문다 - 로그인 화면으로 보내면 다음 실행에 다시 이 화면에 갇힌다.
     */
    fun logout() {
        if (!_termsCheckFailed.value || _loggingOut.value) return
        _loggingOut.value = true
        viewModelScope.launch {
            try {
                authRepository.logout()
                _termsCheckFailed.value = false
                _destination.value = SplashDestination.Login
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "약관 확인 실패 화면에서 로그아웃 실패", e)
                _errorMessage.value = TERMS_CHECK_LOGOUT_FAILED_MESSAGE
            } finally {
                _loggingOut.value = false
            }
        }
    }

    fun consumeError() {
        _errorMessage.value = null
    }

    private fun show(destination: SplashDestination?) {
        if (destination == null) _termsCheckFailed.value = true else _destination.value = destination
    }

    /**
     * 세션이 있으면 서버에 약관 동의 상태를 묻는다. 동의할 약관이 남았으면 동의 화면으로 간다.
     *
     * 확인하지 못하면 null 이다. 홈으로 그냥 보내지도 로그아웃하지도 않고 다시 시도하게 한다.
     */
    private suspend fun decideDestination(): SplashDestination? {
        if (!authRepository.hasSession()) return SplashDestination.Login

        return when (termsRepository.checkAgreement(authRepository)) {
            TermsAgreementCheck.Agreed -> SplashDestination.Main
            TermsAgreementCheck.NeedsAgreement -> SplashDestination.TermsAgreement
            TermsAgreementCheck.Failed -> null
            TermsAgreementCheck.SessionExpired -> {
                clearExpiredSession()
                SplashDestination.Login
            }
        }
    }

    /** 서버가 세션을 끝냈어도 로컬에 남은 것이 있으면 지운다. 남으면 다음 실행에 또 401 을 받는다. */
    private suspend fun clearExpiredSession() {
        try {
            authRepository.logout()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 로컬 세션이 남아도 다시 로그인하면 새로 쓰이므로 로그인 화면으로는 보낸다.
            Log.w(TAG, "세션 만료 뒤 로그아웃 실패", e)
        }
    }
}
