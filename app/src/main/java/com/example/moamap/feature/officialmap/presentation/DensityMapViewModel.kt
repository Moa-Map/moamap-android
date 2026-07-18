package com.example.moamap.feature.officialmap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.feature.officialmap.domain.model.DensityArea
import com.example.moamap.feature.officialmap.domain.repository.FootTrafficRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DensityMapUiState {
    data object Loading : DensityMapUiState
    data class Success(
        val areas: List<DensityArea>,
        val selectedCode: String? = null,
    ) : DensityMapUiState

    data class Error(val message: String) : DensityMapUiState
}

@HiltViewModel
class DensityMapViewModel @Inject constructor(
    private val repository: FootTrafficRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DensityMapUiState>(DensityMapUiState.Loading)
    val uiState: StateFlow<DensityMapUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    /** 지역 탭 선택. 같은 지역을 다시 탭하면 해제한다. */
    fun selectArea(code: String?) {
        _uiState.update { state ->
            if (state !is DensityMapUiState.Success) return@update state
            state.copy(selectedCode = if (state.selectedCode == code) null else code)
        }
    }

    private fun load() {
        _uiState.value = DensityMapUiState.Loading
        viewModelScope.launch {
            runCatching { repository.getDensityAreas() }
                .onSuccess { areas -> _uiState.value = DensityMapUiState.Success(areas) }
                .onFailure { throwable ->
                    _uiState.value = DensityMapUiState.Error(
                        throwable.message ?: "밀집도 정보를 불러오지 못했어요"
                    )
                }
        }
    }
}
