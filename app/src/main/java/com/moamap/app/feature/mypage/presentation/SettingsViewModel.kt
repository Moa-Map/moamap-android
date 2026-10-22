package com.moamap.app.feature.mypage.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.actionableMessage
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"
private const val LOGOUT_ERROR = "로그아웃하지 못했어요. 잠시 후 다시 시도해주세요."
private const val WITHDRAW_ERROR = "탈퇴하지 못했어요. 잠시 후 다시 시도해 주세요."
internal const val TERMS_OPEN_ERROR = "약관을 불러오지 못했어요"

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val termsRepository: TermsRepository,
) : ViewModel() {

    private val _loggedOut = MutableStateFlow(false)

    /** true 가 되면 화면이 로그인으로 이동한다. */
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var inProgress = false

    private val _termsToOpen = MutableStateFlow<Long?>(null)

    /** 열 약관 전문의 termId. 화면이 한 번 읽고 [consumeTermsToOpen] 으로 지운다. */
    val termsToOpen: StateFlow<Long?> = _termsToOpen.asStateFlow()

    private var openingTerms = false

    /**
     * 「이용약관」·「개인정보처리방침」 줄. 약관 번호는 서버가 개정할 때마다 바뀌어 앱에 두지 않고, 지금 약관 목록에서
     * [code] 로 찾는다.
     */
    fun openTerms(code: String) {
        if (openingTerms) return
        openingTerms = true

        viewModelScope.launch {
            try {
                val terms = termsRepository.getTermsList().firstOrNull { it.code == code }
                if (terms == null) {
                    Log.e(TAG, "약관 목록에 $code 가 없음")
                    _errorMessage.value = TERMS_OPEN_ERROR
                } else {
                    _termsToOpen.value = terms.termId
                }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.e(TAG, "약관 목록 조회 실패", throwable)
                _errorMessage.value = throwable.toUserMessage(TERMS_OPEN_ERROR)
            } finally {
                openingTerms = false
            }
        }
    }

    fun consumeTermsToOpen() {
        _termsToOpen.value = null
    }

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

    /** 회원 탈퇴. 성공하면 로그아웃과 같이 로그인 화면으로 간다. 실패하면 세션이 남아 있어 다시 누를 수 있다. */
    fun withdraw() {
        if (_loggedOut.value || inProgress) return
        inProgress = true

        viewModelScope.launch {
            try {
                authRepository.withdraw()
                _loggedOut.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.e(TAG, "회원 탈퇴 실패", throwable)
                // 다른 요청과 겹친 409 는 서버가 「다시 시도해 주세요」를 준다.
                _errorMessage.value = (throwable as? ApiException)?.actionableMessage()
                    ?: throwable.toUserMessage(WITHDRAW_ERROR)
            } finally {
                inProgress = false
            }
        }
    }

    fun consumeError() {
        _errorMessage.value = null
    }
}
