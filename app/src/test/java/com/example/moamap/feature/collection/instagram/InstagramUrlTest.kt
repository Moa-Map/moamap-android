package com.example.moamap.feature.collection.instagram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstagramUrlTest {

    @Test
    fun `릴스 게시물 URL 에서 shortcode 를 뽑는다`() {
        assertEquals("ABC123_x-y", InstagramUrl.shortcodeOf("https://www.instagram.com/reel/ABC123_x-y/"))
    }

    @Test
    fun `p reels tv 경로도 모두 인정한다`() {
        assertEquals("A1", InstagramUrl.shortcodeOf("https://www.instagram.com/p/A1/"))
        assertEquals("A2", InstagramUrl.shortcodeOf("https://www.instagram.com/reels/A2/"))
        assertEquals("A3", InstagramUrl.shortcodeOf("https://www.instagram.com/tv/A3/"))
    }

    @Test
    fun `끝 슬래시가 없어도 인정한다`() {
        assertEquals("ABC123", InstagramUrl.shortcodeOf("https://instagram.com/reel/ABC123"))
    }

    @Test
    fun `공유 링크에 붙는 추적 질의는 무시한다`() {
        assertEquals(
            "DEF456",
            InstagramUrl.shortcodeOf("https://www.instagram.com/reel/DEF456/?igsh=MXAzYnk1ZQ%3D%3D"),
        )
    }

    @Test
    fun `모바일 호스트도 인정한다`() {
        assertEquals("GHI789", InstagramUrl.shortcodeOf("https://m.instagram.com/p/GHI789/"))
    }

    @Test
    fun `프로필 링크는 게시물이 아니다`() {
        assertNull(InstagramUrl.shortcodeOf("https://www.instagram.com/moamap_official/"))
    }

    @Test
    fun `스토리 링크는 지원하지 않는다`() {
        assertNull(InstagramUrl.shortcodeOf("https://www.instagram.com/stories/moamap/123456/"))
    }

    @Test
    fun `호스트가 인스타그램이 아니면 경로가 같아도 거절한다`() {
        assertNull(InstagramUrl.shortcodeOf("https://evil.com/reel/ABC123/"))
        assertNull(InstagramUrl.shortcodeOf("https://instagram.com.evil.com/reel/ABC123/"))
    }

    @Test
    fun `질의 문자열에 섞인 경로는 잡지 않는다`() {
        assertNull(InstagramUrl.shortcodeOf("https://evil.com/?next=/reel/ABC123/"))
    }

    @Test
    fun `http https 가 아니면 거절한다`() {
        assertNull(InstagramUrl.shortcodeOf("javascript://www.instagram.com/reel/ABC123/"))
        assertNull(InstagramUrl.shortcodeOf("www.instagram.com/reel/ABC123/"))
    }

    @Test
    fun `URL 이 아니면 거절한다`() {
        assertNull(InstagramUrl.shortcodeOf(""))
        assertNull(InstagramUrl.shortcodeOf("그냥 텍스트"))
    }
}
