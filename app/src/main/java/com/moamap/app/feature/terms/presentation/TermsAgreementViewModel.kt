package com.moamap.app.feature.terms.presentation

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.core.common.toggle
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.domain.isSessionExpired
import com.moamap.app.feature.terms.domain.model.Terms
import com.moamap.app.feature.terms.domain.model.TermsKind
import com.moamap.app.feature.terms.domain.repository.TERMS_CHANGED_CODE
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.HttpURLConnection.HTTP_CONFLICT
import javax.inject.Inject

private const val TAG = "TermsAgreementVM"

internal const val TERMS_LOAD_FAILED_MESSAGE = "약관을 불러오지 못했어요"
internal const val TERMS_AGREE_FAILED_MESSAGE = "동의하지 못했어요. 잠시 후 다시 시도해 주세요."
internal const val TERMS_CHANGED_MESSAGE = "약관이 바뀌어 다시 확인해 주세요"

/** 동의 화면이 끝난 방식. 화면이 한 번 읽고 [TermsAgreementViewModel.consumeResult] 로 지운다. */
enum class TermsAgreementResult {
    /** 남은 약관에 모두 동의했다. 홈으로 간다. */
    Agreed,

    /** 뒤로 가 로그인을 취소했거나 세션이 끝났다. 세션은 이미 지웠다. */
    Cancelled,
}

@Immutable
data class TermsAgreementUiState(
    val loading: Boolean = true,
    /** 약관을 불러오지 못했다. 안내와 「다시 시도」를 띄운다. */
    val loadErrorMessage: String? = null,
    /** 체크박스 줄. 아직 동의하지 않은 필수 약관(서버 `pendingTerms`), 보여 줄 순서대로. */
    val pendingTerms: List<Terms> = emptyList(),
    /** 체크박스 없이 「‹제목› 보기」 로 붙는 약관(개인정보 처리방침). */
    val viewOnlyTerms: List<Terms> = emptyList(),
    /** 체크한 약관의 termId. */
    val checked: Set<Long> = emptySet(),
    /** 동의를 보내는 중. 아래 버튼을 막는다. */
    val submitting: Boolean = false,
    /** 한 번 띄우는 안내(약관 개정·동의 실패). */
    val message: String? = null,
    val result: TermsAgreementResult? = null,
) {
    /** 「모두 동의하기」 줄의 체크. */
    val allChecked: Boolean
        get() = pendingTerms.isNotEmpty() && pendingTerms.all { term -> term.termId in checked }

    /** 필수 약관을 모두 체크했는지. 그러면 아래 버튼이 「다음으로」 가 된다. */
    val requiredChecked: Boolean
        get() = pendingTerms.isNotEmpty() &&
            pendingTerms.filter { term -> term.kind == TermsKind.REQUIRED }
                .all { term -> term.termId in checked }
}

/**
 * 이용약관 동의. 스플래시와 로그인 직후에 서버가 동의할 약관이 남았다고 하면 온다.
 *
 * 아래 버튼은 필수를 다 체크하기 전에는 「모두 동의하기」(누르면 전부 체크만 한다), 필수를 다 체크하면
 * 「다음으로」(누르면 서버에 동의를 보낸다)다(10-05 사용자 결정).
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
        load()
    }

    /** 「다시 시도」. */
    fun retry() = load()

    /** 동의 상태와 약관 목록을 함께 불러온다. 체크는 처음부터 다시 한다. */
    private fun load(message: String? = null) {
        _uiState.update { state -> state.copy(loading = true, loadErrorMessage = null, checked = emptySet()) }
        viewModelScope.launch {
            try {
                val (status, terms) = coroutineScope {
                    val status = async { termsRepository.getAgreementStatus() }
                    val terms = async { termsRepository.getTermsList() }
                    status.await() to terms.await()
                }
                // 불러오는 사이 뒤로 갔다. 로그아웃이 끝나기 전에 홈으로 가면 그 로그아웃이 중간에 끊긴다.
                if (cancelling) return@launch
                if (!status.needsAgreement) {
                    // 다른 기기에서 이미 동의했다.
                    _uiState.update { state -> state.copy(loading = false, result = TermsAgreementResult.Agreed) }
                    return@launch
                }
                _uiState.update { state ->
                    state.copy(
                        loading = false,
                        pendingTerms = status.pendingTerms,
                        viewOnlyTerms = terms.filter { term -> term.kind == TermsKind.VIEW_ONLY },
                        checked = emptySet(),
                        message = message,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "약관 동의 화면 불러오기 실패", e)
                if (cancelling) return@launch
                if (e.isSessionExpired(authRepository)) {
                    cancel()
                    return@launch
                }
                _uiState.update { state ->
                    state.copy(loading = false, loadErrorMessage = e.toUserMessage(TERMS_LOAD_FAILED_MESSAGE))
                }
            }
        }
    }

    fun toggle(termId: Long) {
        _uiState.update { state ->
            state.copy(checked = state.checked.toggle(termId))
        }
    }

    /** 「모두 동의하기」 줄. 전부 체크돼 있으면 전부 풀고, 아니면 전부 체크한다. */
    fun toggleAll() {
        _uiState.update { state ->
            state.copy(checked = if (state.allChecked) emptySet() else state.allTermIds())
        }
    }

    /** 아래 버튼. 필수를 다 체크하기 전에는 전부 체크만 하고, 다 체크했으면 서버에 동의를 보낸다. */
    fun onBottomButtonClick() {
        val state = _uiState.value
        if (cancelling || state.loading || state.loadErrorMessage != null) return
        if (!state.requiredChecked) {
            _uiState.update { it.copy(checked = it.allTermIds()) }
            return
        }
        if (state.submitting) return
        _uiState.update { it.copy(submitting = true) }
        viewModelScope.launch {
            try {
                val status = termsRepository.agree(state.pendingTerms.map { term -> term.termId })
                if (status.needsAgreement) {
                    // 보낸 사이 다른 약관이 새로 생겼다. 남은 약관으로 다시 그린다.
                    _uiState.update {
                        it.copy(
                            submitting = false,
                            pendingTerms = status.pendingTerms,
                            checked = emptySet(),
                            message = TERMS_CHANGED_MESSAGE,
                        )
                    }
                } else {
                    _uiState.update { it.copy(submitting = false, result = TermsAgreementResult.Agreed) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "약관 동의 실패", e)
                _uiState.update { it.copy(submitting = false) }
                when {
                    e.isSessionExpired(authRepository) -> cancel()
                    e.isTermsChanged() -> load(message = TERMS_CHANGED_MESSAGE)
                    // 서버 문구는 보여 주지 않는다.
                    else -> _uiState.update { it.copy(message = e.toUserMessage(TERMS_AGREE_FAILED_MESSAGE)) }
                }
            }
        }
    }

    /**
     * 뒤로 가기. 로그인을 취소한다(10-05 사용자 결정). 세션이 끝났을 때도 같은 길로 로그인 화면에 간다.
     *
     * 서버·카카오 로그아웃이 실패해도 로컬 세션은 지워지므로([AuthRepository.logout]) 결과와 상관없이 로그인 화면으로
     * 돌아간다.
     *
     * 동의를 보내는 동안에는 무시한다. 보낸 동의가 성공해 홈으로 가면 시작한 로그아웃이 중간에 끊긴다(iOS 와 같다).
     */
    fun cancel() {
        if (cancelling || _uiState.value.submitting) return
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

    fun consumeMessage() {
        _uiState.update { state -> state.copy(message = null) }
    }

    fun consumeResult() {
        _uiState.update { state -> state.copy(result = null) }
    }
}

private fun TermsAgreementUiState.allTermIds(): Set<Long> = pendingTerms.mapTo(HashSet()) { term -> term.termId }

/** 동의 화면을 연 뒤 약관이 개정됐다(409 `USER_023`). */
private fun Throwable.isTermsChanged(): Boolean =
    this is ApiException && (code == TERMS_CHANGED_CODE || status == HTTP_CONFLICT)
