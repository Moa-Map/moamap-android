package com.moamap.app.feature.onboarding.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaSymbolLogo
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import kotlin.math.PI

/** 로고 본체 너비. 피그마 「Splash/」(4925:20009). */
private val SplashLogoWidth = 120.dp

/** 로고 본체 ↔ 「Moa Map」. */
private val SplashLogoTitleGap = 12.dp

private const val SplashTitle = "Moa Map"

/**
 * 배경. 시안 「Splash/」는 다른 파란 바탕 화면(`backgroundPrimary`, 단색 #E6F6FF)과 달리
 * 위 #B3E4FD → 아래 흰색 세로 그라데이션이다.
 */
internal val SplashBackground = Brush.verticalGradient(
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
        playIntro = true,
        modifier = modifier,
    )
}

/**
 * @param termsCheckFailed 약관 동의 여부를 확인하지 못했다. 화면 아래에 안내와 「다시 시도」·「로그아웃」을 띄운다.
 * 로고와 문구는 가운데 그대로 둔다(iOS 와 같다).
 * @param playIntro 로고·글자 등장 애니메이션을 튼다. 미리보기는 다 나온 모습을 보도록 끈다.
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
    playIntro: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground),
    ) {
        SplashLogo(playIntro = playIntro)

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

/**
 * 로고와 「Moa Map」 묶음을 화면 전체의 세로 가운데에 둔다.
 *
 * @param playIntro 처음 나올 때 로고가 커지고 글자가 떠오르며 화면이 일렁인다. 꺼 두면 다 나온 모습으로 그린다(미리보기).
 */
@Composable
private fun SplashLogo(playIntro: Boolean) {
    val introMillis = OnboardingMotion.SplashIntroMillis.toFloat()
    val elapsed = remember { Animatable(if (playIntro) 0f else introMillis) }
    LaunchedEffect(Unit) {
        elapsed.animateTo(introMillis, tween(OnboardingMotion.SplashIntroMillis, easing = LinearEasing))
    }
    val ripple = rememberRippleShader()

    SplashLogoLayout(
        modifier = Modifier.graphicsLayer {
            val strength = 1f - (elapsed.value / OnboardingMotion.RippleMillis).coerceIn(0f, 1f)
            renderEffect = if (ripple != null && strength > 0f) {
                ripple.renderEffect(
                    amplitude = OnboardingMotion.RippleAmplitude.toPx() * strength * strength,
                    wavelength = OnboardingMotion.RippleWavelength.toPx(),
                    phase = elapsed.value / 1_000f * OnboardingMotion.RippleCyclesPerSecond * 2f * PI.toFloat(),
                )
            } else {
                null
            }
        },
        logoModifier = Modifier.graphicsLayer {
            val grow = OnboardingMotion.Overshoot.transform(
                (elapsed.value / OnboardingMotion.LogoMillis).coerceIn(0f, 1f),
            )
            val scale = lerp(OnboardingMotion.LogoStartScale, 1f, grow)
            scaleX = scale
            scaleY = scale
            alpha = (elapsed.value / OnboardingMotion.LogoFadeMillis).coerceIn(0f, 1f)
        },
        titleElapsedMillis = { elapsed.value },
    )
}

/**
 * 스플래시의 로고·「Moa Map」 배치. 로그인 화면도 보이지 않게 그려 두 요소가 스플래시에서 어디 있었는지 잰다 - 거기서 옮겨 온다.
 *
 * @param titleElapsedMillis 「Moa Map」이 한 글자씩 떠오르는 데 흐른 시간. 다 나온 모습이면 [Float.MAX_VALUE].
 */
@Composable
internal fun SplashLogoLayout(
    modifier: Modifier = Modifier,
    logoModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    titleElapsedMillis: () -> Float = { Float.MAX_VALUE },
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(SplashLogoTitleGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MoaSymbolLogo(bodyWidth = SplashLogoWidth, modifier = logoModifier)

        WaveInText(
            text = SplashTitle,
            style = MoaMapTheme.typography.display1,
            color = MoaMapTheme.colors.textNormal,
            elapsedMillis = titleElapsedMillis,
            startMillis = OnboardingMotion.TitleStartMillis,
            staggerMillis = OnboardingMotion.LetterStaggerMillis,
            // 로고가 이미 「모아맵」으로 읽힌다.
            modifier = titleModifier.clearAndSetSemantics {},
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
