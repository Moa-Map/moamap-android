package com.moamap.app.feature.officialmap.presentation

import androidx.compose.ui.graphics.Color
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.feature.officialmap.domain.model.CongestionLevel

internal val CongestionLevel.color: Color
    get() = when (this) {
        CongestionLevel.RELAXED -> MoaMapPrimitiveColors.CongestionRelaxed
        CongestionLevel.NORMAL -> MoaMapPrimitiveColors.CongestionNormal
        CongestionLevel.SLIGHTLY_BUSY -> MoaMapPrimitiveColors.CongestionSlightlyBusy
        CongestionLevel.BUSY -> MoaMapPrimitiveColors.CongestionBusy
        CongestionLevel.UNKNOWN -> MoaMapPrimitiveColors.Gray200
    }

/** 상세 카드 헤더 태그(pill)의 배경·테두리·글자 색. */
internal data class CongestionTagColors(
    val background: Color,
    val border: Color,
    val content: Color,
)

private const val TAG_BACKGROUND_SATURATION = 1.0f
private const val TAG_BACKGROUND_LIGHTNESS = 0.959f
private const val TAG_CONTENT_SATURATION = 0.672f
private const val TAG_CONTENT_LIGHTNESS = 0.251f

internal val CongestionLevel.tagColors: CongestionTagColors
    get() {
        // 회색은 색상 성분이 거의 없어 규칙을 먹이면 엉뚱한 색이 나온다. 중립 토큰을 그대로 쓴다.
        if (this == CongestionLevel.UNKNOWN) {
            return CongestionTagColors(
                background = MoaMapPrimitiveColors.Gray50,
                border = MoaMapPrimitiveColors.Gray200,
                content = MoaMapPrimitiveColors.Gray500,
            )
        }
        val hue = color.hue()
        return CongestionTagColors(
            background = Color.hsl(hue, TAG_BACKGROUND_SATURATION, TAG_BACKGROUND_LIGHTNESS),
            border = color,
            content = Color.hsl(hue, TAG_CONTENT_SATURATION, TAG_CONTENT_LIGHTNESS),
        )
    }

/** RGB → HSL의 명도(0..1) 성분. */
internal fun Color.lightness(): Float =
    (maxOf(red, green, blue) + minOf(red, green, blue)) / 2f

/** RGB → HSL의 색상(0..360) 성분. 무채색이면 0을 돌려준다. */
internal fun Color.hue(): Float {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    if (delta == 0f) return 0f
    val hue = when (max) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }
    return (hue + 360f) % 360f
}
