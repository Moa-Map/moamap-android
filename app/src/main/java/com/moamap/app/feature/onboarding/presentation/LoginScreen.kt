package com.moamap.app.feature.onboarding.presentation

import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/** @param onLoginSuccess 로그인과 약관 동의 상태 확인을 마쳤다. 인자는 동의할 약관이 남았는지다. */
@Composable
fun LoginScreen(
    onLoginSuccess: (needsTermsAgreement: Boolean) -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
    )
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
) {
    // 약관 확인에 실패한 동안에는 세션이 열려 있어 「로그아웃」으로만 빠져나간다.
    val loginEnabled = !isLoading && !termsCheckFailed

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundPrimary),
    ) {
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
                LoginTitleGroup(modifier = Modifier.padding(top = TitleTopInset))

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
                    )
                    SocialLoginButton(
                        iconRes = R.drawable.ic_kakao_logo,
                        label = "카카오톡으로 계속하기",
                        backgroundColor = KakaoYellow,
                        enabled = loginEnabled,
                        onClick = onKakaoClick,
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

/** 「함께 모아, 더 특별해질 지도?」 아래에 로고와 「Moa Map」을 나란히 둔다. */
@Composable
private fun LoginTitleGroup(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = LoginTitle,
            style = MoaMapTheme.typography.subtitle1,
            color = MoaMapTheme.colors.textNormal,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoaSymbolLogo(bodyWidth = LoginLogoWidth)
            Text(
                text = LoginLogoText,
                style = MoaMapTheme.typography.display2,
                color = MoaMapTheme.colors.textNormal,
                // 로고가 이미 「모아맵」으로 읽힌다.
                modifier = Modifier.clearAndSetSemantics {},
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
) {
    Row(
        modifier = Modifier
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
