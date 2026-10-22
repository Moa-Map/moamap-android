package com.moamap.app.feature.terms.presentation

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.feature.terms.domain.model.TermsDetail
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TermsDetailVM"

/** 약관 전문. 읽기 전에는 [terms] 가 비어 있어 상단 바 제목도 비어 있다. */
@Immutable
data class TermsDetailUiState(
    val loading: Boolean = true,
    val terms: TermsDetail? = null,
    val errorMessage: String? = null,
)

/** 약관 하나의 전문. 동의 화면의 항목과 설정 화면의 이용약관·개인정보처리방침이 같이 쓴다. */
@HiltViewModel
class TermsDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val termsRepository: TermsRepository,
) : ViewModel() {

    private val termId: Long = checkNotNull(savedStateHandle[MoaMapRoute.TermsDetail.ARG_TERM_ID])

    private val _uiState = MutableStateFlow(TermsDetailUiState())
    val uiState: StateFlow<TermsDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = TermsDetailUiState()
        viewModelScope.launch {
            _uiState.value = try {
                TermsDetailUiState(loading = false, terms = termsRepository.getTermsDetail(termId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "약관 전문 조회 실패 (id=$termId)", e)
                TermsDetailUiState(loading = false, errorMessage = e.toUserMessage(TERMS_LOAD_FAILED_MESSAGE))
            }
        }
    }
}
