package com.moamap.app.feature.onboarding.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.auth.AgreedTermsVersionStore
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
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
    /** 아직 판단 중. 스플래시를 계속 보여준다. */
    Undecided,
    Login,
    Main,
}

/** 로고가 번쩍이고 사라지지 않도록 보장하는 최소 노출 시간. */
private const val MINIMUM_VISIBLE_MILLIS = 1_500L

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val termsRepository: TermsRepository,
    private val agreedTermsVersionStore: AgreedTermsVersionStore,
) : ViewModel() {

    private val _destination = MutableStateFlow(SplashDestination.Undecided)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            // 판단과 최소 노출 시간을 동시에 돌린다. 판단이 더 오래 걸리면 끝날 때까지 기다린다.
            val destination = async { decideDestination() }
            delay(MINIMUM_VISIBLE_MILLIS)
            _destination.value = destination.await()
        }
    }

    /**
     * 세션이 있어도 지금 약관에 동의한 세션이 아니면 로그아웃하고 로그인 화면으로 보낸다(10-05 사용자
     * 결정). 이 기능 전부터 로그인해 있던 사람, 약관이 바뀐 뒤 처음 켠 사람, 동의 화면에서 앱을 끈
     * 사람이다. 다시 로그인하면 동의 화면을 거친다.
     */
    private suspend fun decideDestination(): SplashDestination {
        if (!authRepository.hasSession()) return SplashDestination.Login
        if (agreedTermsVersionStore.load() == termsRepository.getCurrentVersion()) return SplashDestination.Main

        try {
            authRepository.logout()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 로컬 세션이 남아도 다시 로그인하면 새로 쓰이므로 로그인 화면으로는 보낸다.
            Log.w(TAG, "약관 재동의를 위한 로그아웃 실패", e)
        }
        return SplashDestination.Login
    }
}
