package com.moamap.app.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaSymbolLogo
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/** 로고 본체 너비. 피그마 「Splash/」(4925:20009). */
private val SplashLogoWidth = 120.dp

/** 로고 본체 ↔ 「Moa Map」. */
private val SplashLogoTitleGap = 12.dp

private const val SplashTitle = "Moa Map"

/**
 * 배경. 시안 「Splash/」는 다른 파란 바탕 화면(`backgroundPrimary`, 단색 #E6F6FF)과 달리
 * 위 #B3E4FD → 아래 흰색 세로 그라데이션이다.
 */
private val SplashBackground = Brush.verticalGradient(
    colors = listOf(MoaMapPrimitiveColors.Blue100, MoaMapPrimitiveColors.White),
)

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToTermsAgreement: () -> Unit,
    onNavigateToMain: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val termsCheckFailed by viewModel.termsCheckFailed.collectAsStateWithLifecycle()
    val loggingOut by viewModel.loggingOut.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (destination) {
            SplashDestination.Undecided -> Unit
            SplashDestination.Login -> onNavigateToLogin()
            SplashDestination.TermsAgreement -> onNavigateToTermsAgreement()
            SplashDestination.Main -> onNavigateToMain()
        }
    }

    SplashContent(
        termsCheckFailed = termsCheckFailed,
        loggingOut = loggingOut,
        errorMessage = errorMessage,
        onRetryClick = viewModel::retry,
        onLogoutClick = viewModel::logout,
        onErrorShown = viewModel::consumeError,
        modifier = modifier,
    )
}

/**
 * @param termsCheckFailed 약관 동의 여부를 확인하지 못했다. 화면 아래에 안내와 「다시 시도」·「로그아웃」을 띄운다.
 * 로고와 문구는 가운데 그대로 둔다(iOS 와 같다).
 */
@Composable
internal fun SplashContent(
    termsCheckFailed: Boolean,
    loggingOut: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground),
    ) {
        SplashLogo()

        if (termsCheckFailed) {
            TermsCheckFailedNotice(
                loggingOut = loggingOut,
                onRetryClick = onRetryClick,
                onLogoutClick = onLogoutClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            )
        }

        ErrorSnackbar(
            message = errorMessage,
            onShown = onErrorShown,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

/** 로고와 「Moa Map」 묶음을 화면 전체의 세로 가운데에 둔다. */
@Composable
private fun SplashLogo() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(SplashLogoTitleGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MoaSymbolLogo(bodyWidth = SplashLogoWidth)

        Text(
            text = SplashTitle,
            style = MoaMapTheme.typography.display1,
            color = MoaMapTheme.colors.textNormal,
            // 로고가 이미 「모아맵」으로 읽힌다.
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SplashScreenPreview() {
    MoaMapTheme {
        SplashContent(
            termsCheckFailed = false,
            loggingOut = false,
            errorMessage = null,
            onRetryClick = {},
            onLogoutClick = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SplashScreenTermsCheckFailedPreview() {
    MoaMapTheme {
        SplashContent(
            termsCheckFailed = true,
            loggingOut = false,
            errorMessage = null,
            onRetryClick = {},
            onLogoutClick = {},
            onErrorShown = {},
        )
    }
}
