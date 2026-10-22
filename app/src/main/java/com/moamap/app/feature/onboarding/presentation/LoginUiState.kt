package com.moamap.app.feature.onboarding.presentation

sealed interface LoginUiState {

    data object Idle : LoginUiState

    /** 소셜 로그인부터 약관 동의 상태 확인까지. 버튼을 막는다. */
    data object Loading : LoginUiState

    /**
     * 화면 이동을 위한 일회성 신호. 소비한 뒤 [Idle] 로 되돌려 중복 이동을 막는다.
     *
     * @param needsTermsAgreement 동의할 약관이 남았다. 동의 화면으로 간다. 아니면 홈으로 간다.
     */
    data class Success(val needsTermsAgreement: Boolean) : LoginUiState

    /**
     * 로그인은 됐는데 약관 동의 여부를 확인하지 못했다. 화면 아래쪽에 안내와 「다시 시도」·「로그아웃」을 띄우고 로그인
     * 버튼은 막는다 - 빠져나가려면 「로그아웃」을 누른다(iOS 와 같다).
     *
     * @param loggingOut 「로그아웃」을 진행 중. 「다시 시도」·「로그아웃」을 다시 누를 수 없다.
     * @param logoutErrorMessage 로그아웃하지 못했다는 한 번짜리 안내. 화면에 머문다.
     */
    data class TermsCheckFailed(
        val loggingOut: Boolean = false,
        val logoutErrorMessage: String? = null,
    ) : LoginUiState

    data class Error(val message: String) : LoginUiState
}
