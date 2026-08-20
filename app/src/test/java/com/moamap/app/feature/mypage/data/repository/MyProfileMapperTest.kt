package com.moamap.app.feature.mypage.data.repository

import com.moamap.app.feature.mypage.data.remote.MyPageDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MyProfileMapperTest {

    @Test
    fun `널로 온 문자열은 빈 문자열로 좁힌다`() {
        val profile = MyPageDto(id = 7).toMyProfile()

        assertEquals(7L, profile.id)
        assertEquals("", profile.nickname)
        assertEquals("", profile.email)
        assertEquals("", profile.introduction)
    }

    @Test
    fun `빈 프로필 이미지 주소는 없는 것과 같게 본다`() {
        val profile = MyPageDto(id = 1, profileImageUrl = "   ").toMyProfile()

        assertNull(profile.profileImageUrl)
    }

    @Test
    fun `채워져 온 응답은 그대로 옮긴다`() {
        val dto = MyPageDto(
            id = 3,
            nickname = "모아",
            email = "moa@example.com",
            profileImageUrl = "https://cdn.example.com/a.png",
            provider = "kakao",
            role = "USER",
            introduction = "안녕하세요",
        )

        val profile = dto.toMyProfile()

        assertEquals("모아", profile.nickname)
        assertEquals("moa@example.com", profile.email)
        assertEquals("https://cdn.example.com/a.png", profile.profileImageUrl)
        assertEquals("안녕하세요", profile.introduction)
    }
}
