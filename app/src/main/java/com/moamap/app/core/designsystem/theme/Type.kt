package com.moamap.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.moamap.app.R

private val NanumSquare = FontFamily(
    Font(R.font.nanum_square_light, FontWeight.Light),
    Font(R.font.nanum_square_regular, FontWeight.Normal),
    Font(R.font.nanum_square_bold, FontWeight.Bold),
    Font(R.font.nanum_square_extra_bold, FontWeight.ExtraBold),
)

private val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
)

@Immutable
data class MoaMapTypography(
    val display1: TextStyle,
    val display2: TextStyle,
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val subtitle1: TextStyle,
    val subtitle2: TextStyle,
    val subtitle3: TextStyle,
    val subtitle4: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val body3: TextStyle,
    val button0: TextStyle,
    val button1: TextStyle,
    val button2: TextStyle,
    val button3: TextStyle,
    val button4: TextStyle,
    val caption0: TextStyle,
    val caption1: TextStyle,
    val caption2: TextStyle,
)

private fun nanumSquareStyle(
    size: Int,
    weight: FontWeight,
    lineHeightRatio: Float,
    letterSpacing: Double,
) = TextStyle(
    fontFamily = NanumSquare,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeightRatio).sp,
    letterSpacing = letterSpacing.sp,
)

internal val MoaMapTypographyTokens = MoaMapTypography(
    display1 = nanumSquareStyle(32, FontWeight.ExtraBold, 1.3f, 0.0),
    display2 = nanumSquareStyle(28, FontWeight.ExtraBold, 1.3f, 0.0),
    title1 = nanumSquareStyle(24, FontWeight.ExtraBold, 1.3f, -0.48),
    title2 = nanumSquareStyle(20, FontWeight.ExtraBold, 1.3f, -0.40),
    title3 = nanumSquareStyle(18, FontWeight.ExtraBold, 1.3f, -0.36),
    subtitle1 = nanumSquareStyle(18, FontWeight.Bold, 1.3f, -0.36),
    subtitle2 = nanumSquareStyle(16, FontWeight.Bold, 1.3f, -0.32),
    subtitle3 = nanumSquareStyle(16, FontWeight.ExtraBold, 1.3f, -0.32),
    subtitle4 = nanumSquareStyle(15, FontWeight.Bold, 1.3f, -0.30),
    body1 = nanumSquareStyle(15, FontWeight.Normal, 1.5f, -0.30),
    body2 = nanumSquareStyle(14, FontWeight.Normal, 1.5f, -0.28),
    body3 = nanumSquareStyle(14, FontWeight.Bold, 1.5f, -0.28),
    button0 = nanumSquareStyle(16, FontWeight.Bold, 1.4f, -0.32),
    button1 = nanumSquareStyle(16, FontWeight.Normal, 1.4f, -0.32),
    button2 = nanumSquareStyle(14, FontWeight.Bold, 1.3f, 0.0),
    button3 = nanumSquareStyle(14, FontWeight.Normal, 1.3f, -0.28),
    button4 = nanumSquareStyle(12, FontWeight.Normal, 1.4f, -0.24),
    caption0 = nanumSquareStyle(12, FontWeight.Normal, 1.3f, -0.24),
    caption1 = nanumSquareStyle(12, FontWeight.Light, 1.3f, -0.24),
    caption2 = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.3.sp,
        letterSpacing = (-0.22).sp,
    ),
)

/**
 * 지정한 line height 여백을 그대로 살린다.
 *
 * Compose 기본값([LineHeightStyle.Trim.Both])은 첫 줄 위와 마지막 줄 아래 여백을 잘라내서,
 * 한 줄짜리 텍스트가 line height 가 아니라 글자 높이만큼만 자리를 차지한다. 그 결과 여러 줄을
 * 세로로 쌓으면 디자인보다 줄 간격이 좁아진다. 피그마 좌표를 그대로 맞춰야 하는 곳에서 쓴다.
 */
fun TextStyle.withDesignLineHeight(): TextStyle = copy(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Proportional,
        trim = LineHeightStyle.Trim.None,
    ),
)

internal val MoaMapMaterialTypography = Typography(
    displayLarge = MoaMapTypographyTokens.display1,
    displayMedium = MoaMapTypographyTokens.display2,
    displaySmall = MoaMapTypographyTokens.title1,
    headlineLarge = MoaMapTypographyTokens.title1,
    headlineMedium = MoaMapTypographyTokens.title2,
    headlineSmall = MoaMapTypographyTokens.title3,
    titleLarge = MoaMapTypographyTokens.subtitle1,
    titleMedium = MoaMapTypographyTokens.subtitle2,
    titleSmall = MoaMapTypographyTokens.subtitle4,
    bodyLarge = MoaMapTypographyTokens.body1,
    bodyMedium = MoaMapTypographyTokens.body2,
    bodySmall = MoaMapTypographyTokens.caption0,
    labelLarge = MoaMapTypographyTokens.button0,
    labelMedium = MoaMapTypographyTokens.button2,
    labelSmall = MoaMapTypographyTokens.caption2,
)
