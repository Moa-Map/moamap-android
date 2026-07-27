package com.example.moamap.feature.mypage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.feature.onboarding.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _loggedOut = MutableStateFlow(false)

    /** true 가 되면 화면이 로그인으로 이동한다. */
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    fun logout() {
        if (_loggedOut.value) return

        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // 저장소 정리까지 실패해도 사용자를 이 화면에 가둬두지 않는다.
                // 세션이 남아 있다면 다음 로그인에서 덮어써진다.
            }
            _loggedOut.value = true
        }
    }
}
