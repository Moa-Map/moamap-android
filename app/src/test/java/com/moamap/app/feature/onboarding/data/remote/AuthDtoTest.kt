package com.moamap.app.feature.onboarding.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `로그인 응답에서 사용자 식별자를 읽는다`() {
        val token = json.decodeFromString<TokenDto>(
            """{"userId":42,"accessToken":"a","refreshToken":"r","tokenType":"Bearer",
               "expiresIn":3600,"refreshTokenExpiresIn":1209600,"isNewUser":false}""",
        )

        assertEquals(42L, token.userId)
        assertEquals("a", token.accessToken)
        assertEquals("r", token.refreshToken)
    }

    /**
     * 갱신 응답에는 식별자가 없다. 서버 배포가 앱보다 늦어도 로그인이 막히면 안 되므로,
     * 빠져 있으면 "모른다"는 뜻의 0 으로 둔다.
     */
    @Test
    fun `식별자가 없으면 0 이다`() {
        val token = json.decodeFromString<TokenDto>(
            """{"accessToken":"a","refreshToken":"r"}""",
        )

        assertEquals(0L, token.userId)
    }
}
