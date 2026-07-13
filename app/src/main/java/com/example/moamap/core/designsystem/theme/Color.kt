package com.example.moamap.core.designsystem.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal object MoaMapPrimitiveColors {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val TransparentBlack = Color(0xBF000000)

    val Gray50 = Color(0xFFECEDED)
    val Gray100 = Color(0xFFC5C7C7)
    val Gray200 = Color(0xFFA9ABAC)
    val Gray300 = Color(0xFF828586)
    val Gray400 = Color(0xFF696D6F)
    val Gray500 = Color(0xFF4A4F52)
    val Gray600 = Color(0xFF3E4244)
    val Gray700 = Color(0xFF303435)
    val Gray800 = Color(0xFF252829)
    val Gray900 = Color(0xFF1D1F20)

    val Yellow50 = Color(0xFFFFFAEA)
    val Yellow100 = Color(0xFFFFEEBF)
    val Yellow200 = Color(0xFFFFE6A0)
    val Yellow300 = Color(0xFFFFDA75)
    val Yellow400 = Color(0xFFFFD35A)
    val Yellow500 = Color(0xFFFFC831)
    val Yellow600 = Color(0xFFE8B62D)
    val Yellow700 = Color(0xFFB58E23)
    val Yellow800 = Color(0xFF8C6E1B)
    val Yellow900 = Color(0xFF6B5415)

    val Blue50 = Color(0xFFE6F6FF)
    val Blue100 = Color(0xFFB3E4FD)
    val Blue200 = Color(0xFF8ED7FD)
    val Blue300 = Color(0xFF5AC5FC)
    val Blue400 = Color(0xFF3AB9FB)
    val Blue500 = Color(0xFF09A8FA)
    val Blue600 = Color(0xFF0899E4)
    val Blue700 = Color(0xFF0677B2)
    val Blue800 = Color(0xFF055C8A)
    val Blue900 = Color(0xFF044769)

    val TextNormal = Color(0xFF151617)
    val BackgroundSecondary = Color(0xFFF7F9FA)
    val StatusAlert = Color(0xFFFB1921)
    val StatusCaution = Color(0xFFFC912F)
    val StatusPositive = Color(0xFF1E9E6A)
    val LineNormal = Color(0xFFD5DBDB)
}

@Immutable
data class MoaMapColors(
    val primary: Color,
    val secondary: Color,
    val textNormal: Color,
    val textAlternative: Color,
    val textAssistive: Color,
    val textDisable: Color,
    val textWhite: Color,
    val backgroundPrimary: Brush,
    val backgroundSecondary: Color,
    val statusAlert: Color,
    val statusCaution: Color,
    val statusPositive: Color,
    val lineNormal: Color,
    val lineAlternative: Color,
)

internal val MoaMapLightColors = MoaMapColors(
    primary = MoaMapPrimitiveColors.Blue500,
    secondary = MoaMapPrimitiveColors.Yellow500,
    textNormal = MoaMapPrimitiveColors.TextNormal,
    textAlternative = MoaMapPrimitiveColors.Gray500,
    textAssistive = MoaMapPrimitiveColors.Gray300,
    textDisable = MoaMapPrimitiveColors.Gray100,
    textWhite = MoaMapPrimitiveColors.White,
    backgroundPrimary = Brush.verticalGradient(
        colors = listOf(MoaMapPrimitiveColors.Blue100, MoaMapPrimitiveColors.White),
    ),
    backgroundSecondary = MoaMapPrimitiveColors.BackgroundSecondary,
    statusAlert = MoaMapPrimitiveColors.StatusAlert,
    statusCaution = MoaMapPrimitiveColors.StatusCaution,
    statusPositive = MoaMapPrimitiveColors.StatusPositive,
    lineNormal = MoaMapPrimitiveColors.LineNormal,
    lineAlternative = MoaMapPrimitiveColors.Gray50,
)

internal val MoaMapMaterialColorScheme = lightColorScheme(
    primary = MoaMapPrimitiveColors.Blue500,
    onPrimary = MoaMapPrimitiveColors.White,
    primaryContainer = MoaMapPrimitiveColors.Blue50,
    onPrimaryContainer = MoaMapPrimitiveColors.Blue900,
    secondary = MoaMapPrimitiveColors.Yellow500,
    onSecondary = MoaMapPrimitiveColors.TextNormal,
    secondaryContainer = MoaMapPrimitiveColors.Yellow50,
    onSecondaryContainer = MoaMapPrimitiveColors.Yellow900,
    background = MoaMapPrimitiveColors.White,
    onBackground = MoaMapPrimitiveColors.TextNormal,
    surface = MoaMapPrimitiveColors.White,
    onSurface = MoaMapPrimitiveColors.TextNormal,
    surfaceVariant = MoaMapPrimitiveColors.BackgroundSecondary,
    onSurfaceVariant = MoaMapPrimitiveColors.Gray500,
    error = MoaMapPrimitiveColors.StatusAlert,
    onError = MoaMapPrimitiveColors.White,
    outline = MoaMapPrimitiveColors.LineNormal,
    outlineVariant = MoaMapPrimitiveColors.Gray50,
)
