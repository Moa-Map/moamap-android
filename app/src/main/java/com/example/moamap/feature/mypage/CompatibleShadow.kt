package com.example.moamap.feature.mypage

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

internal fun Modifier.compatibleShadow(
    shape: Shape,
    blurRadius: Dp,
    color: Color,
): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    blur(
        radius = blurRadius,
        edgeTreatment = BlurredEdgeTreatment.Unbounded,
    ).background(
        color = color,
        shape = shape,
    )
} else {
    shadow(
        elevation = blurRadius,
        shape = shape,
        clip = false,
        ambientColor = color,
        spotColor = color,
    ).background(
        color = color,
        shape = shape,
    )
}
