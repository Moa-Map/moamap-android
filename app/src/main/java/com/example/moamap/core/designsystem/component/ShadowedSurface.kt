package com.example.moamap.core.designsystem.component

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors

/**
 * 피그마 카드 그림자 `0 0 8 rgba(0,0,0,0.04)`.
 *
 * Material 의 `shadowElevation` 은 같은 blur 값이라도 훨씬 진하게 그려져서 디자인과 어긋난다.
 * 피그마의 blur/알파를 그대로 재현하려면 [ShadowedSurface] 를 쓴다.
 */
val CardShadowBlurRadius: Dp = 8.dp
val CardShadowColor: Color = MoaMapPrimitiveColors.Black.copy(alpha = 0.04f)

/** 피그마 버튼 그림자 `0 0 10 rgba(0,0,0,0.1)`. */
val ButtonShadowBlurRadius: Dp = 10.dp
val ButtonShadowColor: Color = MoaMapPrimitiveColors.Black.copy(alpha = 0.1f)

/**
 * 그림자를 콘텐츠 뒤에 따로 깔아 피그마의 blur/알파를 그대로 재현하는 컨테이너.
 *
 * Android 12 이상은 [blur] 로, 그 아래는 [shadow] 로 처리한다.
 */
fun Modifier.compatibleShadow(
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

/**
 * 피그마 그림자를 그대로 쓰는 카드 컨테이너.
 *
 * `Surface(shadowElevation = ...)` 는 Material 규격의 진한 그림자를 그려서,
 * 알파가 4~8% 인 디자인 그림자와 눈에 띄게 다르다.
 */
@Composable
fun ShadowedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    color: Color = MoaMapPrimitiveColors.White,
    shadowBlurRadius: Dp = CardShadowBlurRadius,
    shadowColor: Color = CardShadowColor,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    // 바깥 Box 의 크기는 콘텐츠가 정하고, 그림자 Box 는 matchParentSize 로 그 크기를 따라간다.
    Box(modifier = modifier, propagateMinConstraints = true) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .compatibleShadow(
                    shape = shape,
                    blurRadius = shadowBlurRadius,
                    color = shadowColor,
                ),
        )
        Box(
            modifier = Modifier
                .clip(shape)
                .background(color = color, shape = shape)
                .let { base -> if (border == null) base else base.border(border, shape) }
                .let { base -> if (onClick == null) base else base.clickable(onClick = onClick) },
            // 콘텐츠가 표면 크기를 물려받게 한다. 이게 없으면 가로를 채우는 표면 안에서
            // 콘텐츠만 제 크기로 줄어들어 왼쪽에 붙는다(예: 버튼 안 글자).
            propagateMinConstraints = true,
            content = content,
        )
    }
}
