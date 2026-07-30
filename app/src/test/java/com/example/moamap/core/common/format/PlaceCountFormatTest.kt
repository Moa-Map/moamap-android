package com.example.moamap.core.common.format

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceCountFormatTest {

    @Test
    fun `천 단위 미만은 그대로 붙인다`() {
        assertEquals("0곳", formatPlaceCount(0))
        assertEquals("116곳", formatPlaceCount(116))
        assertEquals("999곳", formatPlaceCount(999))
    }

    @Test
    fun `천 단위부터 구분자를 넣는다`() {
        assertEquals("1,000곳", formatPlaceCount(1_000))
        assertEquals("5,416곳", formatPlaceCount(5_416))
        assertEquals("1,234,567곳", formatPlaceCount(1_234_567))
    }
}
