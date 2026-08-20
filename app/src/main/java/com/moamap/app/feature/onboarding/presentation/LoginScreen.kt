package com.moamap.app.feature.onboarding.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/** 피그마 918:1219 (393 x 852) 기준 값. */
private val LoginLogoSize = DpSize(width = 173.dp, height = 103.dp)
private val SocialButtonHeight = 53.dp
private val SocialButtonShape = RoundedCornerShape(12.dp)
private val SocialButtonIconSize = 24.dp
private val SocialButtonGap = 16.dp

/**
 * 세로 여백 비율. 393 x 852 기준에서는 디자인 좌표와 일치하고, 다른 화면 비율에서도
 * 버튼이 잘리지 않게 늘어난다.
 */
private const val TopSpacerWeight = 239f
private const val MiddleSpacerWeight = 69f
private const val BottomSpacerWeight = 247f

/** 카카오 브랜드 색. 디자인 시스템 팔레트가 아니라서 토큰으로 승격하지 않는다. */
private val KakaoYellow = Color(0xFFFEE500)

/** 소셜 버튼 라벨 색. 피그마의 rgba(0, 0, 0, 0.85). */
private const val SocialButtonLabelAlpha = 0.85f

private const val LoginTitle = "취향을 담아,\n우리만의 지도로"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    // 카카오 SDK 가 로그인 화면을 띄우려면 Activity 컨텍스트가 필요하다.
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState == LoginUiState.Success) {
            viewModel.consumeSuccess()
            onLoginSuccess()
        }
    }

    LoginContent(
        isLoading = uiState == LoginUiState.Loading,
        errorMessage = (uiState as? LoginUiState.Error)?.message,
        onKakaoClick = { viewModel.loginWithKakao(context) },
        onErrorShown = viewModel::consumeError,
        modifier = modifier,
    )
}

@Composable
internal fun LoginContent(
    isLoading: Boolean,
    errorMessage: String?,
    onKakaoClick: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundPrimary),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(TopSpacerWeight))

            Image(
                painter = painterResource(R.drawable.img_moa_logo),
                contentDescription = "모아맵",
                modifier = Modifier.size(LoginLogoSize),
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = LoginTitle,
                style = MoaMapTheme.typography.title1,
                color = MoaMapPrimitiveColors.Black,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(MiddleSpacerWeight))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 서버에 구글 로그인 API 가 없어 이번 이슈에서는 렌더링만 한다.
                SocialLoginButton(
                    iconRes = R.drawable.ic_google_logo,
                    label = "구글로 계속하기",
                    backgroundColor = MoaMapPrimitiveColors.White,
                    enabled = false,
                    onClick = {},
                )
                SocialLoginButton(
                    iconRes = R.drawable.ic_kakao_logo,
                    label = "카카오톡으로 계속하기",
                    backgroundColor = KakaoYellow,
                    enabled = !isLoading,
                    onClick = onKakaoClick,
                )
            }

            Spacer(Modifier.weight(BottomSpacerWeight))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
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
            onKakaoClick = {},
            onErrorShown = {},
        )
    }
}
