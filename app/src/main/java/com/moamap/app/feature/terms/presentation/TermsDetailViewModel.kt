package com.moamap.app.feature.terms.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 약관 전문. 읽기 전에는 제목이 비어 있다. */
data class TermsDetailUiState(
    val title: String = "",
    val sections: List<TermsSection> = emptyList(),
)

/** 약관 하나의 전문. 동의 화면의 항목과 설정 화면의 이용약관·개인정보처리방침이 같이 쓴다. */
@HiltViewModel
class TermsDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val termsRepository: TermsRepository,
) : ViewModel() {

    private val code: String = checkNotNull(savedStateHandle[MoaMapRoute.TermsDetail.ARG_CODE])

    private val _uiState = MutableStateFlow(TermsDetailUiState())
    val uiState: StateFlow<TermsDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val terms = termsRepository.getTerms(code)
            _uiState.value = TermsDetailUiState(
                title = terms.title,
                sections = parseTermsSections(terms.content),
            )
        }
    }
}
