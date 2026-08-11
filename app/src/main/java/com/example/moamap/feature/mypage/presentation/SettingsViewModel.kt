package com.example.moamap.feature.mypage.presentation

import android.util.Log
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

private const val TAG = "SettingsViewModel"
private const val LOGOUT_ERROR = "로그아웃하지 못했어요. 잠시 후 다시 시도해주세요."

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _loggedOut = MutableStateFlow(false)

    /** true 가 되면 화면이 로그인으로 이동한다. */
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var inProgress = false

    fun logout() {
        if (_loggedOut.value || inProgress) return
        inProgress = true

        viewModelScope.launch {
            try {
                authRepository.logout()
                _loggedOut.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                // 로컬 토큰이 남았을 수 있다. 로그아웃됐다고 알리면 앱을 다시 켰을 때
                // 스플래시가 남은 세션을 읽어 메인으로 보내므로 사용자를 속이게 된다.
                Log.e(TAG, "로그아웃 실패", throwable)
                _errorMessage.value = LOGOUT_ERROR
            } finally {
                inProgress = false
            }
        }
    }

    fun consumeError() {
        _errorMessage.value = null
    }
}
