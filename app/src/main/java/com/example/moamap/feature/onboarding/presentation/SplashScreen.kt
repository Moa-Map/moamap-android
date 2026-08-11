package com.example.moamap.feature.onboarding.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

/** 피그마 918:1193 (393 x 852) 기준 로고 크기. */
private val SplashLogoSize = DpSize(width = 229.dp, height = 137.dp)

/**
 * 세로 여백 비율. 고정 dp 로 박으면 작은 화면에서 문구가 잘리므로 비율로 나눈다.
 * 393 x 852 기준에서는 디자인 좌표와 일치한다.
 */
private const val TopSpacerWeight = 259f
private const val BottomSpacerWeight = 370f

private const val SplashTitle = "취향을 담아,\n우리만의 지도로"

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
            .background(MoaMapTheme.colors.backgroundPrimary),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(TopSpacerWeight))

        Image(
            painter = painterResource(R.drawable.img_moa_logo),
            contentDescription = "모아맵",
            modifier = Modifier.size(SplashLogoSize),
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = SplashTitle,
            style = MoaMapTheme.typography.display2,
            color = MoaMapPrimitiveColors.Black,
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
