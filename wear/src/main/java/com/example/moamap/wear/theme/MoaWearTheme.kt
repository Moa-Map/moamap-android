package com.example.moamap.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * 앱과 같은 색.
 *
 * `core/designsystem` 은 `app` 모듈 안에 있어 여기서 참조할 수 없다. 값이 앱의
 * `theme/Color.kt` 와 **같아야 한다** - 한쪽만 바꾸면 두 앱이 다른 서비스처럼 보인다.
 */
internal object MoaWearColors {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Blue200 = Color(0xFF8ED7FD)
    val Blue500 = Color(0xFF09A8FA)
    val Blue900 = Color(0xFF044769)
    val Yellow500 = Color(0xFFFFC831)
    val Gray200 = Color(0xFFA9ABAC)
    val StatusAlert = Color(0xFFFB1921)
}

/**
 * 배경은 검정을 쓴다.
 *
 * 앱의 하늘색→흰색 그라데이션을 그대로 옮기면 OLED 워치에서 밤에 눈부시고 상시 표시에서
 * 배터리를 그만큼 더 쓴다. 색 계열은 배경이 아니라 primary 로 잇는다.
 */
private val MoaWearColorScheme = ColorScheme(
    primary = MoaWearColors.Blue500,
    onPrimary = MoaWearColors.White,
    primaryContainer = MoaWearColors.Blue900,
    onPrimaryContainer = MoaWearColors.Blue200,
    secondary = MoaWearColors.Yellow500,
    onSecondary = MoaWearColors.Black,
    background = MoaWearColors.Black,
    onBackground = MoaWearColors.White,
    surfaceContainer = MoaWearColors.Black,
    onSurface = MoaWearColors.White,
    onSurfaceVariant = MoaWearColors.Gray200,
    error = MoaWearColors.StatusAlert,
    onError = MoaWearColors.White,
)

@Composable
fun MoaWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MoaWearColorScheme, content = content)
}
