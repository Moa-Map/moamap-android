package com.example.moamap.feature.officialmap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.feature.officialmap.domain.model.CongestionLevel
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
        /** null이면 "전체" — 모든 레벨을 보여준다. */
        val filterLevel: CongestionLevel? = null,
    ) : DensityMapUiState {
        /** 지도와 카드가 함께 바라보는, 필터를 통과한 지역 목록. */
        val visibleAreas: List<DensityArea>
            get() = if (filterLevel == null) areas
            else areas.filter { it.congestion?.level == filterLevel }

        val selectedArea: DensityArea?
            get() = visibleAreas.firstOrNull { it.code == selectedCode }
    }

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

    /** 레벨 필터 선택. 같은 레벨을 다시 고르면 전체로 돌아간다. */
    fun selectLevel(level: CongestionLevel?) {
        _uiState.update { state ->
            if (state !is DensityMapUiState.Success) return@update state
            val next = state.copy(filterLevel = if (state.filterLevel == level) null else level)
            // 보고 있던 지역이 필터 밖으로 나가면 하단 카드도 함께 닫는다.
            if (next.selectedArea == null) next.copy(selectedCode = null) else next
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
