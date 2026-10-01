package com.moamap.app.core.designsystem.component

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class BelowAnchorPositionTest {

    private val anchor = IntRect(left = 300, top = 10, right = 400, bottom = 60)
    private val popup = IntSize(width = 200, height = 50)

    private fun BelowAnchorPosition.place() =
        calculatePosition(anchor, IntSize(1080, 2000), LayoutDirection.Ltr, popup)

    @Test
    fun `기본은 왼쪽 끝을 기준 요소에 맞춰 바로 아래에 띄운다`() {
        assertEquals(IntOffset(288, 63), BelowAnchorPosition(IntOffset(-12, 3)).place())
    }

    @Test
    fun `alignEnd 면 팝업 오른쪽 끝을 기준 요소 오른쪽 끝에 맞춘다`() {
        // 오른쪽 끝 400 - 팝업 폭 200 = 200
        assertEquals(
            IntOffset(200, 50),
            BelowAnchorPosition(IntOffset(0, -10), alignEnd = true).place(),
        )
    }
}
