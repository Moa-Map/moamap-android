package com.moamap.app.feature.onboarding.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingMotionTest {

    @Test
    fun `글자는 차례로 늦게 시작해 각자 정한 시간 동안 나온다`() {
        fun progress(elapsed: Float, index: Int) =
            staggeredProgress(elapsed, index, startMillis = 100, staggerMillis = 50, durationMillis = 200)

        assertEquals(0f, progress(elapsed = 50f, index = 0))
        assertEquals(0.5f, progress(elapsed = 200f, index = 0))
        assertEquals(0f, progress(elapsed = 150f, index = 1))
        assertEquals(0.25f, progress(elapsed = 200f, index = 1))
        assertEquals(1f, progress(elapsed = 1_000f, index = 3))
    }

    @Test
    fun `전체 시간은 마지막 글자가 끝나는 때다`() {
        assertEquals(1_030, staggeredTotalMillis(count = 7, startMillis = 250, staggerMillis = 55, durationMillis = 450))
        assertEquals(450, staggeredTotalMillis(count = 0, startMillis = 0, staggerMillis = 55, durationMillis = 450))
    }

    @Test
    fun `스플래시 등장은 최소 노출 시간 안에 끝난다`() {
        val title = staggeredTotalMillis(
            count = "Moa Map".length,
            startMillis = OnboardingMotion.TitleStartMillis,
            staggerMillis = OnboardingMotion.LetterStaggerMillis,
            durationMillis = OnboardingMotion.LetterMillis,
        )
        assert(title <= OnboardingMotion.SplashIntroMillis)
        assert(OnboardingMotion.SplashIntroMillis < 1_500)
    }
}
