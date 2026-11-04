package com.moamap.app.feature.onboarding.presentation

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * 스플래시 → 로그인 애니메이션의 시간 값(ms). 모션 시안이 없어 흔히 쓰는 움직임으로 정했고, iOS 가 같은 값으로 따라간다.
 *
 * 스플래시: 로고가 0.6배에서 살짝 넘치며 커지고([LogoMillis]), 「Moa Map」은 [TitleStartMillis] 부터 한 글자씩
 * [LetterStaggerMillis] 간격으로 떠오른다([LetterMillis]). 그동안 화면이 물결처럼 일렁이다 잦아든다([RippleMillis]).
 * 로그인 화면이 열리면 로고와 글씨가 [ArrivalMillis] 동안 작아지며 자리로 옮겨 가고, 끝나면 문구가 같은 방식으로
 * 떠오른 뒤([LoginTitleStaggerMillis]) [ButtonsDelayMillis] 뒤에 버튼이 하나씩 올라온다.
 */
internal object OnboardingMotion {
    const val LogoMillis = 700
    const val LogoFadeMillis = 300
    const val LogoStartScale = 0.6f

    const val TitleStartMillis = 250
    const val LetterStaggerMillis = 55
    const val LetterMillis = 450

    const val RippleMillis = 1_000

    /** 일렁임의 처음 세기·띠 간격·초당 흐르는 띠 수. 세기는 [RippleMillis] 동안 제곱으로 잦아든다. */
    val RippleAmplitude: Dp = 8.dp
    val RippleWavelength: Dp = 90.dp
    const val RippleCyclesPerSecond = 1.2f

    /** 스플래시 등장 전체. 화면은 최소 1.5초 머물러([SplashViewModel]) 등장이 끝난 뒤 넘어간다. */
    const val SplashIntroMillis = 1_100

    const val ArrivalMillis = 700

    const val LoginTitleStaggerMillis = 30
    const val ButtonsDelayMillis = 350
    const val ButtonStaggerMillis = 80
    const val ButtonMillis = 450

    /** 글자·버튼이 떠오를 때 아래에서 올라오는 거리. */
    val RiseDistance: Dp = 14.dp

    /** 끝에서 살짝 넘쳤다 돌아오는 곡선(easeOutBack). 떠오르는 움직임에 탄력을 준다. */
    val Overshoot = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
}

/**
 * [elapsedMillis] 시점에 [index] 번째 글자(또는 버튼)가 얼마나 나왔는지(0~1). [startMillis] 에 첫 글자가 시작하고
 * 다음 글자는 [staggerMillis] 씩 늦게 시작해 각각 [durationMillis] 동안 나온다.
 */
internal fun staggeredProgress(
    elapsedMillis: Float,
    index: Int,
    startMillis: Int,
    staggerMillis: Int,
    durationMillis: Int,
): Float = ((elapsedMillis - startMillis - index * staggerMillis) / durationMillis).coerceIn(0f, 1f)

/** 글자 수가 [count] 일 때 마지막 글자까지 다 나오는 시간. */
internal fun staggeredTotalMillis(count: Int, startMillis: Int, staggerMillis: Int, durationMillis: Int): Int =
    startMillis + (count - 1).coerceAtLeast(0) * staggerMillis + durationMillis

/**
 * 한 글자씩 아래에서 떠오르며 나타나는 글자. [elapsedMillis] 는 그리기 단계에서만 읽어 매 프레임 다시 구성하지 않는다.
 *
 * 글자를 따로 그려 읽기 도구는 낱자로 읽는다. 그래서 묶어서 [text] 하나로 읽게 한다 - 꾸밈이면 호출하는 쪽이 지운다.
 */
@Composable
internal fun WaveInText(
    text: String,
    style: TextStyle,
    color: Color,
    elapsedMillis: () -> Float,
    startMillis: Int,
    staggerMillis: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.clearAndSetSemantics { contentDescription = text }) {
        text.forEachIndexed { index, char ->
            Text(
                text = char.toString(),
                style = style,
                color = color,
                modifier = Modifier.graphicsLayer {
                    val progress = staggeredProgress(
                        elapsedMillis(), index, startMillis, staggerMillis, OnboardingMotion.LetterMillis,
                    )
                    alpha = progress
                    translationY = (1f - OnboardingMotion.Overshoot.transform(progress)) *
                        OnboardingMotion.RiseDistance.toPx()
                },
            )
        }
    }
}

/**
 * 화면을 물결처럼 일렁이게 한다. 가로로 흔들리는 띠가 위아래로 흐르며 [amplitude] 만큼 밀린다.
 * 안드로이드 13 미만은 셰이더가 없어 일렁임 없이 글자·로고 등장만 보인다.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class RippleShader {
    private val shader = RuntimeShader(
        """
        uniform shader content;
        uniform float amplitude;
        uniform float wavelength;
        uniform float phase;
        half4 main(float2 p) {
            float dx = sin(p.y / wavelength * 6.2831853 + phase) * amplitude;
            float dy = sin(p.x / wavelength * 6.2831853 + phase * 0.7) * amplitude * 0.4;
            return content.eval(float2(p.x + dx, p.y + dy));
        }
        """.trimIndent(),
    )

    fun renderEffect(amplitude: Float, wavelength: Float, phase: Float) =
        android.graphics.RenderEffect.createRuntimeShaderEffect(
            shader.apply {
                setFloatUniform("amplitude", amplitude)
                setFloatUniform("wavelength", wavelength)
                setFloatUniform("phase", phase)
            },
            "content",
        ).asComposeRenderEffect()
}

/** 스플래시 일렁임의 셰이더. 안드로이드 13 미만은 null. */
@Composable
internal fun rememberRippleShader(): RippleShader? = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RippleShader() else null
}

/**
 * 스플래시에 있던 자리([from])에서 지금 자리([to])로 옮겨 오며 크기를 맞춘다. [progress] 0 이면 스플래시 자리·크기, 1 이면 제자리다.
 * 두 자리는 화면 기준 좌표이고, 아직 재지 못했으면 옮기기 전까지 숨긴다.
 *
 * 컴포즈의 공유 요소 전환을 쓰지 않는다. 그 API 는 실험 단계라 버전마다 모양이 바뀌는데, 디버그 빌드는 ui-tooling 때문에
 * 컴파일한 것(애니메이션 1.9)보다 새 버전(1.10)으로 돌아 부르는 순간 죽는다.
 */
internal fun Modifier.arriveFrom(from: () -> Rect?, to: () -> Rect?, progress: () -> Float) = graphicsLayer {
    val fraction = progress()
    if (fraction >= 1f) return@graphicsLayer
    val start = from()
    val end = to()
    if (start == null || end == null || end.width == 0f) {
        alpha = 0f
        return@graphicsLayer
    }
    val eased = FastOutSlowInEasing.transform(fraction)
    val scale = lerp(start.width / end.width, 1f, eased)
    scaleX = scale
    scaleY = scale
    translationX = lerp(start.center.x - end.center.x, 0f, eased)
    translationY = lerp(start.center.y - end.center.y, 0f, eased)
}
