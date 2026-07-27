package com.example.moamap.feature.officialmap.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.moamap.feature.officialmap.domain.model.CongestionLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class CongestionStyleTest {

    private fun Color.hex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

    @Test
    fun `레벨 색이 Figma 값과 일치한다`() {
        assertEquals("#FB1921", CongestionLevel.BUSY.color.hex())
        assertEquals("#FC912F", CongestionLevel.SLIGHTLY_BUSY.color.hex())
        assertEquals("#F8CE34", CongestionLevel.NORMAL.color.hex())
        assertEquals("#95E5AB", CongestionLevel.RELAXED.color.hex())
    }

    @Test
    fun `보통 태그 색이 Figma Yellow 테마를 재현한다`() {
        val tag = CongestionLevel.NORMAL.tagColors

        assertEquals("#FFFBEA", tag.background.hex())
        assertEquals("#F8CE34", tag.border.hex())
        assertEquals("#6B5915", tag.content.hex())
    }

    @Test
    fun `나머지 레벨 태그 색이 같은 규칙으로 파생된다`() {
        val busy = CongestionLevel.BUSY.tagColors
        assertEquals("#FFEAEB", busy.background.hex())
        assertEquals("#FB1921", busy.border.hex())
        assertEquals("#6B1518", busy.content.hex())

        val relaxed = CongestionLevel.RELAXED.tagColors
        assertEquals("#EAFFF0", relaxed.background.hex())
        assertEquals("#95E5AB", relaxed.border.hex())
        assertEquals("#156B2D", relaxed.content.hex())
    }
}
