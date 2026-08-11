package com.example.moamap.feature.officialmap.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.moamap.feature.officialmap.domain.model.CongestionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CongestionStyleTest {

    private fun Color.hex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

    @Test
    fun `레벨 색이 혼잡도 램프 토큰과 일치한다`() {
        assertEquals("#D01620", CongestionLevel.BUSY.color.hex())
        assertEquals("#EA6A17", CongestionLevel.SLIGHTLY_BUSY.color.hex())
        assertEquals("#E0A21A", CongestionLevel.NORMAL.color.hex())
        assertEquals("#10B39A", CongestionLevel.RELAXED.color.hex())
    }

    @Test
    fun `램프 4색의 명도가 고른 구간에 모여 있다`() {
        // 네 단계가 같은 무게로 읽히려면 명도가 흩어지면 안 된다.
        val levels = listOf(
            CongestionLevel.RELAXED,
            CongestionLevel.NORMAL,
            CongestionLevel.SLIGHTLY_BUSY,
            CongestionLevel.BUSY,
        )

        levels.forEach { level ->
            val lightness = level.color.lightness()
            assertTrue(
                "${level.label} 명도가 구간을 벗어났다: $lightness",
                lightness in 0.35f..0.55f,
            )
        }
    }

    @Test
    fun `여유 색이 베이스맵 녹지 색상대에서 벗어나 있다`() {
        // Mapbox Standard의 공원·산 녹지는 대략 hue 90~130이다.
        // 여유가 그 대역에 있으면 지형처럼 보인다.
        val hue = CongestionLevel.RELAXED.color.hue()

        assertTrue("여유 hue가 녹지 대역에 너무 가깝다: $hue", hue > 150f)
    }

    @Test
    fun `보통 태그 색이 base 색 hue에서 파생된다`() {
        val tag = CongestionLevel.NORMAL.tagColors

        assertEquals("#FFF8EA", tag.background.hex())
        assertEquals("#E0A21A", tag.border.hex())
        assertEquals("#6B5015", tag.content.hex())
    }

    @Test
    fun `정보 없음 태그는 무채색을 유지한다`() {
        // 회색은 hue가 0이라 채도를 그대로 먹이면 빨강 계열이 되어 경고처럼 보인다.
        val unknown = CongestionLevel.UNKNOWN.tagColors

        assertEquals("#ECEDED", unknown.background.hex())
        assertEquals("#A9ABAC", unknown.border.hex())
        assertEquals("#4A4F52", unknown.content.hex())
    }

    @Test
    fun `나머지 레벨 태그 색이 같은 규칙으로 파생된다`() {
        val busy = CongestionLevel.BUSY.tagColors
        assertEquals("#FFEAEB", busy.background.hex())
        assertEquals("#D01620", busy.border.hex())
        assertEquals("#6B151A", busy.content.hex())

        val slightlyBusy = CongestionLevel.SLIGHTLY_BUSY.tagColors
        assertEquals("#FFF2EA", slightlyBusy.background.hex())
        assertEquals("#EA6A17", slightlyBusy.border.hex())
        assertEquals("#6B3715", slightlyBusy.content.hex())

        val relaxed = CongestionLevel.RELAXED.tagColors
        assertEquals("#EAFFFC", relaxed.background.hex())
        assertEquals("#10B39A", relaxed.border.hex())
        assertEquals("#156B5E", relaxed.content.hex())
    }
}
