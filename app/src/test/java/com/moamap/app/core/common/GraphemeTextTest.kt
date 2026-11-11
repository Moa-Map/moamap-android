package com.moamap.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

/** 이슈 #418 의 기준 예시. iOS 도 같은 입력에 같은 결과를 내야 한다. */
class GraphemeTextTest {

    private val ga = "가"
    private val flag = "🇰🇷" // 🇰🇷 UTF-16 4
    private val thumbsMedium = "👍🏽" // 👍🏽 UTF-16 4
    private val family = "👨‍👩‍👧" // 👨‍👩‍👧 UTF-16 8
    private val grin = "😀" // 😀 UTF-16 2

    @Test
    fun `기준 예시 - 한도에 걸친 이모지는 통째로 빠진다`() {
        assertEquals(ga.repeat(99), (ga.repeat(99) + grin).takeWithinUtf16(100))
        assertEquals(ga.repeat(98) + grin, (ga.repeat(98) + grin).takeWithinUtf16(100))
        assertEquals(ga.repeat(98), (ga.repeat(98) + family).takeWithinUtf16(100))
        assertEquals(ga.repeat(96) + flag, (ga.repeat(96) + flag).takeWithinUtf16(100))
        assertEquals(ga.repeat(97), (ga.repeat(97) + flag).takeWithinUtf16(100))
        assertEquals(grin.repeat(50), grin.repeat(51).takeWithinUtf16(100))
        assertEquals("", thumbsMedium.takeWithinUtf16(3))
        assertEquals("a", ("a" + thumbsMedium).takeWithinUtf16(4))
        assertEquals("abc가나다", "abc가나다".takeWithinUtf16(100))
    }

    @Test
    fun `한도와 길이가 같으면 그대로 둔다`() {
        val exact = ga.repeat(96) + flag

        assertEquals(exact, exact.takeWithinUtf16(100))
    }

    @Test
    fun `자른 결과에 서로게이트 반쪽이 남지 않는다`() {
        val input = ga.repeat(97) + grin + grin

        val result = input.takeWithinUtf16(100)

        assertEquals(ga.repeat(97) + grin, result)
        assertEquals(false, Character.isHighSurrogate(result.last()))
    }

    @Test
    fun `한도 0 이면 빈 문자열이다`() {
        assertEquals("", "abc".takeWithinUtf16(0))
    }
}
