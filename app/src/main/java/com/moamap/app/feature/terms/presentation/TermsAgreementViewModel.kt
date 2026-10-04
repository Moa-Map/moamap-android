package com.moamap.app.feature.terms.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.domain.model.Terms
import com.moamap.app.feature.terms.domain.model.TermsConsentType
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TermsAgreementVM"

/** 동의 화면이 끝난 방식. 화면이 한 번 읽고 [TermsAgreementViewModel.consumeResult] 로 지운다. */
enum class TermsAgreementResult {
    /** 필수 약관에 모두 동의했다. 로그인을 마친다. */
    Agreed,

    /** 뒤로 가 로그인을 취소했다. 세션은 이미 지웠다. */
    Cancelled,
}

data class TermsAgreementUiState(
    /** 동의 화면에 나오는 약관(필수·선택), 보여 줄 순서대로. */
    val terms: List<Terms> = emptyList(),
    /** 체크한 약관의 코드. */
    val checked: Set<String> = emptySet(),
    val result: TermsAgreementResult? = null,
) {
    /** 「모두 동의하기」 줄의 체크. 선택 약관까지 전부 체크했을 때. */
    val allChecked: Boolean
        get() = terms.isNotEmpty() && terms.all { term -> term.code in checked }

    /** 필수 약관을 모두 체크했는지. 그러면 아래 버튼이 「다음으로」 가 된다(선택은 안 해도 된다). */
    val requiredChecked: Boolean
        get() = terms.isNotEmpty() &&
            terms.filter { term -> term.consentType == TermsConsentType.REQUIRED }
                .all { term -> term.code in checked }
}

/**
 * 소셜 로그인 다음의 이용약관 동의.
 *
 * 아래 버튼은 필수를 다 체크하기 전에는 「모두 동의하기」(누르면 전부 체크만 한다), 필수를 다 체크하면
 * 「다음으로」(누르면 로그인을 마친다)다(10-05 사용자 결정).
 *
 * 동의 기록은 아직 남기지 않는다. 약관 API 가 없어 서버에 남길 곳이 없고, 테스트할 때 매번 동의
 * 화면을 보려고 휴대폰에도 남기지 않는다(10-05 사용자 결정). 그래서 로그인할 때마다 이 화면이 뜬다.
 */
@HiltViewModel
class TermsAgreementViewModel @Inject constructor(
    private val termsRepository: TermsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TermsAgreementUiState())
    val uiState: StateFlow<TermsAgreementUiState> = _uiState.asStateFlow()

    /** 로그인 취소(로그아웃)를 진행 중인지. 뒤로가기를 연타해도 한 번만 한다. */
    private var cancelling = false

    init {
        viewModelScope.launch {
            val terms = termsRepository.getAgreementTerms()
            _uiState.update { state -> state.copy(terms = terms) }
        }
    }

    fun toggle(code: String) {
        _uiState.update { state ->
            state.copy(checked = if (code in state.checked) state.checked - code else state.checked + code)
        }
    }

    /** 「모두 동의하기」 줄. 전부 체크돼 있으면 전부 풀고, 아니면 전부 체크한다. */
    fun toggleAll() {
        _uiState.update { state ->
            state.copy(checked = if (state.allChecked) emptySet() else state.terms.mapTo(HashSet()) { it.code })
        }
    }

    /** 아래 버튼. 필수를 다 체크하기 전에는 전부 체크만 하고, 다 체크했으면 로그인을 마친다. */
    fun onBottomButtonClick() {
        _uiState.update { state ->
            if (state.requiredChecked) {
                state.copy(result = TermsAgreementResult.Agreed)
            } else {
                state.copy(checked = state.terms.mapTo(HashSet()) { it.code })
            }
        }
    }

    /**
     * 뒤로 가기. 로그인을 취소한다(10-05 사용자 결정).
     *
     * 소셜 로그인 때 받은 토큰이 이미 저장돼 있어 로그아웃으로 지운다. 서버·카카오 로그아웃이 실패해도
     * 로컬 세션은 지워지므로([AuthRepository.logout]) 결과와 상관없이 로그인 화면으로 돌아간다.
     */
    fun cancel() {
        if (cancelling) return
        cancelling = true
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "약관 동의 취소 중 로그아웃 실패", e)
            }
            _uiState.update { state -> state.copy(result = TermsAgreementResult.Cancelled) }
        }
    }

    fun consumeResult() {
        _uiState.update { state -> state.copy(result = null) }
    }
}
