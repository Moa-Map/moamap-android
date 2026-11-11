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

    @Test
    fun `기준 예시 - 이모지 거르기`() {
        assertEquals("ab@c.com", "a${grin}b@c.com".removeEmoji())
        assertEquals("https://naver.me/xy", "https://naver.me/x${flag}y".removeEmoji())
        assertEquals("2#*", "1️⃣2#*".removeEmoji())
        assertEquals("ab", "a${family}b".removeEmoji())
    }

    @Test
    fun `피부색 이모지와 기본이 이모지인 BMP 글자도 거른다`() {
        assertEquals("ab", "a${thumbsMedium}b".removeEmoji())
        assertEquals("ab", "a✅⭐b".removeEmoji()) // ✅⭐
        assertEquals("ab", "a❤️b".removeEmoji()) // ❤️
    }

    @Test
    fun `이모지가 없으면 그대로 둔다`() {
        val input = "https://map.naver.com/p/entry/place/123?c=15.00,0,0,0,dh#a*b 한글"

        assertEquals(input, input.removeEmoji())
    }
}
