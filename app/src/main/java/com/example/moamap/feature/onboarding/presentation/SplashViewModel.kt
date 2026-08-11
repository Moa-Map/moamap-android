package com.example.moamap.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.feature.onboarding.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
) : ViewModel() {

    private val _destination = MutableStateFlow(SplashDestination.Undecided)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            // 세션 조회와 최소 노출 시간을 동시에 돌린다. 조회가 더 오래 걸리면 끝날 때까지 기다린다.
            val hasSession = async { authRepository.hasSession() }
            delay(MINIMUM_VISIBLE_MILLIS)
            _destination.value = if (hasSession.await()) {
                SplashDestination.Main
            } else {
                SplashDestination.Login
            }
        }
    }
}
