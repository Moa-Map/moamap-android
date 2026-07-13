package com.example.moamap.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalMoaMapColors = staticCompositionLocalOf { MoaMapLightColors }
private val LocalMoaMapTypography = staticCompositionLocalOf { MoaMapTypographyTokens }

object MoaMapTheme {
    val colors: MoaMapColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMoaMapColors.current

    val typography: MoaMapTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalMoaMapTypography.current
}

@Composable
fun MoaMapTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMoaMapColors provides MoaMapLightColors,
        LocalMoaMapTypography provides MoaMapTypographyTokens,
    ) {
        MaterialTheme(
            colorScheme = MoaMapMaterialColorScheme,
            typography = MoaMapMaterialTypography,
            content = content,
        )
    }
}
