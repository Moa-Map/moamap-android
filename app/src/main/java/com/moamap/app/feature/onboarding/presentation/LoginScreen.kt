package com.moamap.app.feature.onboarding.presentation

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaSymbolLogo
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/** 피그마 「Home/」(4925:19953) 기준 값. */
private val LoginLogoWidth = 60.dp
private val SocialButtonHeight = 53.dp
private val SocialButtonShape = RoundedCornerShape(12.dp)
private val SocialButtonIconSize = 24.dp
private val SocialButtonGap = 16.dp

/** 화면 위 ↔ 문구 묶음, 버튼 묶음 ↔ 화면 아래. 안전 영역이 아니라 화면 전체 기준이다. */
private val TitleTopInset = 200.dp
private val ButtonsBottomInset = 118.dp

/** 화면이 낮아도 문구와 버튼 사이는 이만큼 비운다. 그보다 낮으면 스크롤된다. */
private val TitleButtonsMinGap = 40.dp

/** 카카오 브랜드 색. 디자인 시스템 팔레트가 아니라서 토큰으로 승격하지 않는다. */
private val KakaoYellow = Color(0xFFFEE500)

/** 소셜 버튼 라벨 색. 피그마의 rgba(0, 0, 0, 0.85). */
private const val SocialButtonLabelAlpha = 0.85f

private const val LoginTitle = "함께 모아, 더 특별해질 지도?"
private const val LoginLogoText = "Moa Map"

/**
 * @param onLoginSuccess 로그인과 약관 동의 상태 확인을 마쳤다. 인자는 동의할 약관이 남았는지다.
 * @param arrivedFromSplash 스플래시에서 바로 왔다. 스플래시의 로고와 「Moa Map」이 작아지며 이 화면 자리로 옮겨 온다.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (needsTermsAgreement: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    arrivedFromSplash: Boolean = false,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    // 카카오 SDK·Credential Manager 가 로그인 화면을 띄우려면 Activity 컨텍스트가 필요하다.
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        val success = uiState as? LoginUiState.Success ?: return@LaunchedEffect
        viewModel.consumeSuccess()
        onLoginSuccess(success.needsTermsAgreement)
    }

    val termsCheckFailed = uiState as? LoginUiState.TermsCheckFailed
    val elapsed = rememberLoginIntro(arrivedFromSplash)
    val arrivalMillis = if (arrivedFromSplash) OnboardingMotion.ArrivalMillis.toFloat() else 0f

    LoginContent(
        isLoading = uiState == LoginUiState.Loading,
        errorMessage = (uiState as? LoginUiState.Error)?.message ?: termsCheckFailed?.logoutErrorMessage,
        termsCheckFailed = termsCheckFailed != null,
        loggingOut = termsCheckFailed?.loggingOut == true,
        onGoogleClick = { viewModel.loginWithGoogle(context) },
        onKakaoClick = { viewModel.loginWithKakao(context) },
        onErrorShown = viewModel::consumeError,
        onRetryTermsCheckClick = viewModel::retryTermsCheck,
        onLogoutClick = viewModel::logoutAfterTermsCheckFailed,
        arrivalProgress = {
            if (arrivalMillis == 0f) 1f else (elapsed.value / arrivalMillis).coerceIn(0f, 1f)
        },
        introElapsedMillis = { elapsed.value - arrivalMillis },
        modifier = modifier,
    )
}

/** 문구와 버튼이 다 나오는 시간. */
private val LoginIntroMillis = maxOf(
    staggeredTotalMillis(LoginTitle.length, 0, OnboardingMotion.LoginTitleStaggerMillis, OnboardingMotion.LetterMillis),
    OnboardingMotion.ButtonsDelayMillis + OnboardingMotion.ButtonStaggerMillis + OnboardingMotion.ButtonMillis,
)

/**
 * 화면이 열린 뒤 흐른 시간(ms). 스플래시에서 왔으면 처음 [OnboardingMotion.ArrivalMillis] 동안 로고와 글씨가 옮겨 오고,
 * 그 뒤 문구와 버튼이 차례로 나온다. 로그아웃해 왔으면 옮겨 오기 없이 바로 문구부터 나온다. 화면을 돌려도 다시 틀지 않는다.
 */
@Composable
private fun rememberLoginIntro(arrivedFromSplash: Boolean): Animatable<Float, AnimationVector1D> {
    val totalMillis = LoginIntroMillis + if (arrivedFromSplash) OnboardingMotion.ArrivalMillis else 0
    var played by rememberSaveable { mutableStateOf(false) }
    val elapsed = remember { Animatable(if (played) totalMillis.toFloat() else 0f) }
    LaunchedEffect(Unit) {
        if (played) return@LaunchedEffect
        elapsed.animateTo(totalMillis.toFloat(), tween(totalMillis, easing = LinearEasing))
        played = true
    }
    return elapsed
}

@Composable
internal fun LoginContent(
    isLoading: Boolean,
    errorMessage: String?,
    termsCheckFailed: Boolean,
    loggingOut: Boolean,
    onGoogleClick: () -> Unit,
    onKakaoClick: () -> Unit,
    onErrorShown: () -> Unit,
    onRetryTermsCheckClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    arrivalProgress: () -> Float = { 1f },
    introElapsedMillis: () -> Float = { Float.MAX_VALUE },
) {
    // 버튼이 떠오르기 전에는 누르지 못하게 한다.
    val buttonsShown by remember(introElapsedMillis) {
        derivedStateOf { introElapsedMillis() >= OnboardingMotion.ButtonsDelayMillis }
    }
    // 약관 확인에 실패한 동안에는 세션이 열려 있어 「로그아웃」으로만 빠져나간다.
    val loginEnabled = !isLoading && !termsCheckFailed && buttonsShown

    // 스플래시에서 옮겨 오는 동안 두 화면에서 로고와 「Moa Map」이 있는 자리(화면 기준).
    val arriving by remember(arrivalProgress) { derivedStateOf { arrivalProgress() < 1f } }
    var splashLogoBounds by remember { mutableStateOf<Rect?>(null) }
    var splashTitleBounds by remember { mutableStateOf<Rect?>(null) }
    var logoBounds by remember { mutableStateOf<Rect?>(null) }
    var titleBounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundPrimary)
            // 스플래시 바탕에서 이 화면 바탕으로 서서히 바뀐다.
            .drawBehind {
                val splashAlpha = 1f - arrivalProgress()
                if (splashAlpha > 0f) drawRect(SplashBackground, alpha = splashAlpha)
            },
    ) {
        if (arriving) {
            SplashLogoLayout(
                modifier = Modifier
                    .alpha(0f)
                    .clearAndSetSemantics {},
                logoModifier = Modifier.onGloballyPositioned { coordinates ->
                    splashLogoBounds = coordinates.boundsInRoot()
                },
                titleModifier = Modifier.onGloballyPositioned { coordinates ->
                    splashTitleBounds = coordinates.boundsInRoot()
                },
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 시안 위치를 지키되, 낮은 화면에서는 버튼이 잘리지 않게 스크롤된다.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LoginTitleGroup(
                    introElapsedMillis = introElapsedMillis,
                    logoModifier = Modifier
                        .onGloballyPositioned { coordinates -> logoBounds = coordinates.boundsInRoot() }
                        .arriveFrom({ splashLogoBounds }, { logoBounds }, arrivalProgress),
                    titleModifier = Modifier
                        .onGloballyPositioned { coordinates -> titleBounds = coordinates.boundsInRoot() }
                        .arriveFrom({ splashTitleBounds }, { titleBounds }, arrivalProgress),
                    modifier = Modifier.padding(top = TitleTopInset),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TitleButtonsMinGap, bottom = ButtonsBottomInset),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SocialLoginButton(
                        iconRes = R.drawable.ic_google_logo,
                        label = "구글로 계속하기",
                        backgroundColor = MoaMapPrimitiveColors.White,
                        enabled = loginEnabled,
                        onClick = onGoogleClick,
                        modifier = Modifier.riseIn(introElapsedMillis, index = 0),
                    )
                    SocialLoginButton(
                        iconRes = R.drawable.ic_kakao_logo,
                        label = "카카오톡으로 계속하기",
                        backgroundColor = KakaoYellow,
                        enabled = loginEnabled,
                        onClick = onKakaoClick,
                        modifier = Modifier.riseIn(introElapsedMillis, index = 1),
                    )
                }
            }
        }

        // 로그인 직후 약관 동의 여부를 확인하지 못했다. 버튼 아래 여백에 띄워 버튼 자리는 움직이지 않는다.
        if (termsCheckFailed) {
            TermsCheckFailedNotice(
                loggingOut = loggingOut,
                onRetryClick = onRetryTermsCheckClick,
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

/** 버튼이 [index] 차례에 아래에서 떠오르며 나타난다. */
private fun Modifier.riseIn(elapsedMillis: () -> Float, index: Int) = graphicsLayer {
    val progress = staggeredProgress(
        elapsedMillis(),
        index,
        OnboardingMotion.ButtonsDelayMillis,
        OnboardingMotion.ButtonStaggerMillis,
        OnboardingMotion.ButtonMillis,
    )
    alpha = progress
    translationY = (1f - OnboardingMotion.Overshoot.transform(progress)) * OnboardingMotion.RiseDistance.toPx()
}

/**
 * 「함께 모아, 더 특별해질 지도?」 아래에 로고와 「Moa Map」을 나란히 둔다. 로고와 「Moa Map」은 스플래시에서 옮겨 오고,
 * 문구는 그 뒤 한 글자씩 떠오른다. 문구 자리는 처음부터 잡아 둬 로고가 옮겨 갈 자리가 흔들리지 않는다.
 */
@Composable
private fun LoginTitleGroup(
    introElapsedMillis: () -> Float,
    logoModifier: Modifier,
    titleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WaveInText(
            text = LoginTitle,
            style = MoaMapTheme.typography.subtitle1,
            color = MoaMapTheme.colors.textNormal,
            elapsedMillis = introElapsedMillis,
            startMillis = 0,
            staggerMillis = OnboardingMotion.LoginTitleStaggerMillis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoaSymbolLogo(bodyWidth = LoginLogoWidth, modifier = logoModifier)
            Text(
                text = LoginLogoText,
                style = MoaMapTheme.typography.display2,
                color = MoaMapTheme.colors.textNormal,
                // 로고가 이미 「모아맵」으로 읽힌다.
                modifier = titleModifier.clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun SocialLoginButton(
    @DrawableRes iconRes: Int,
    label: String,
    backgroundColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SocialButtonHeight)
            .clip(SocialButtonShape)
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(SocialButtonIconSize),
        )
        Spacer(Modifier.width(SocialButtonGap))
        Text(
            text = label,
            style = MoaMapTheme.typography.button0,
            color = MoaMapPrimitiveColors.Black.copy(alpha = SocialButtonLabelAlpha),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun LoginScreenPreview() {
    MoaMapTheme {
        LoginContent(
            isLoading = false,
            errorMessage = null,
            termsCheckFailed = false,
            loggingOut = false,
            onGoogleClick = {},
            onKakaoClick = {},
            onErrorShown = {},
            onRetryTermsCheckClick = {},
            onLogoutClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun LoginScreenTermsCheckFailedPreview() {
    MoaMapTheme {
        LoginContent(
            isLoading = false,
            errorMessage = null,
            termsCheckFailed = true,
            loggingOut = false,
            onGoogleClick = {},
            onKakaoClick = {},
            onErrorShown = {},
            onRetryTermsCheckClick = {},
            onLogoutClick = {},
        )
    }
}
