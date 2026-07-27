package com.example.moamap.feature.onboarding.presentation

sealed interface LoginUiState {

    data object Idle : LoginUiState

    data object Loading : LoginUiState

    /** 화면 이동을 위한 일회성 신호. 소비한 뒤 [Idle] 로 되돌려 중복 이동을 막는다. */
    data object Success : LoginUiState

    data class Error(val message: String) : LoginUiState
}
