package com.moamap.app.core.designsystem.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode

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

/**
 * 아무것도 그리지 않는 누름 효과.
 *
 * 누를 때 회색이 번지는 기본 리플을 앱 전체에서 쓰지 않는다. 텍스트 정렬 버튼처럼 배경이
 * 없는 요소에 네모난 회색이 떠서 시안과 어긋나 보였다.
 */
private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = System.identityHashCode(this)
}

@OptIn(ExperimentalMaterial3Api::class)
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
        ) {
            // MaterialTheme 이 리플을 깔기 때문에 그 안쪽에서 덮어써야 한다.
            // LocalIndication 은 Modifier.clickable 이, LocalRippleConfiguration 은
            // Button·Card 같은 Material 컴포넌트가 본다. 둘 다 꺼야 앱 전체에서 사라진다.
            CompositionLocalProvider(
                LocalIndication provides NoIndication,
                LocalRippleConfiguration provides null,
                content = content,
            )
        }
    }
}
