package com.moamap.app.core.common.format

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PlaceCountFormatTest {

    private val defaultLocale: Locale = Locale.getDefault()

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

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

    /**
     * 기기 로케일에 맡기면 마침표로 묶는 곳이 있다. 문구가 한국어라 `"5.416곳"` 은
     * 자릿수 구분이 아니라 소수점으로 읽힌다.
     */
    @Test
    fun `기기 로케일이 달라도 쉼표로 묶는다`() {
        Locale.setDefault(Locale.GERMANY)

        assertEquals("5,416곳", formatPlaceCount(5_416))
    }
}
