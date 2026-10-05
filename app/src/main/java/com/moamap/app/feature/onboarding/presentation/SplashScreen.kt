package com.moamap.app.feature.onboarding.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

/**
 * 로고 그림 크기. 피그마 3552:18809 (393 x 852)의 로고 본체는 108.5 x 93 인데, 그림자가 그림 밖으로
 * 번져 그림이 더 크다. 본체는 그림 안 왼쪽 1.5, 위 0 에서 시작한다.
 */
private val SplashLogoSize = DpSize(width = 110.dp, height = 95.75.dp)

/** 본체가 화면 가운데 오도록 그림 안 왼쪽 여백의 절반만큼 당긴다. */
private val SplashLogoOffsetX = (-0.75).dp

/** 로고 본체 ↔ 문구 20. 그림 아래쪽 그림자 여백(2.75)만큼 뺀다. */
private val SplashLogoTitleGap = 17.25.dp

/**
 * 세로 여백 비율. 고정 dp 로 박으면 작은 화면에서 문구가 잘리므로 비율로 나눈다.
 * 393 x 852 기준에서는 디자인 좌표와 일치한다.
 */
private const val TopSpacerWeight = 334f
private const val BottomSpacerWeight = 332f

private const val SplashTitle = "취향을 담아,\n우리만의 지도로"

/**
 * 배경. 시안 「Splash/」(3552:18930)는 다른 파란 바탕 화면(`backgroundPrimary`, 단색 #E6F6FF)과 달리
 * 위 #B3E4FD → 아래 흰색 세로 그라데이션이다.
 */
private val SplashBackground = Brush.verticalGradient(
    colors = listOf(MoaMapPrimitiveColors.Blue100, MoaMapPrimitiveColors.White),
)

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (destination) {
            SplashDestination.Undecided -> Unit
            SplashDestination.Login -> onNavigateToLogin()
            SplashDestination.Main -> onNavigateToMain()
        }
    }

    SplashContent(modifier = modifier)
}

@Composable
internal fun SplashContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(TopSpacerWeight))

        Image(
            painter = painterResource(R.drawable.img_moa_symbol_large),
            contentDescription = "모아맵",
            modifier = Modifier
                .offset(x = SplashLogoOffsetX)
                .size(SplashLogoSize),
        )

        Spacer(Modifier.height(SplashLogoTitleGap))

        Text(
            text = SplashTitle,
            style = MoaMapTheme.typography.display2,
            color = MoaMapTheme.colors.textNormal,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(BottomSpacerWeight))
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SplashScreenPreview() {
    MoaMapTheme {
        SplashContent()
    }
}
