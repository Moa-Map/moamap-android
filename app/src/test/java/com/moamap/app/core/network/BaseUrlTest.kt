package com.moamap.app.core.network

import com.moamap.app.BuildConfig
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseUrlTest {

    @Test
    fun `BASE_URL은 슬래시로 끝난다`() {
        // Retrofit 은 baseUrl 이 / 로 끝나지 않으면 IllegalArgumentException 을 던진다.
        assertTrue(
            "BASE_URL=${BuildConfig.BASE_URL}",
            BuildConfig.BASE_URL.endsWith("/"),
        )
    }

    @Test
    fun `BASE_URL은 https 절대 주소다`() {
        // targetSdk 28 부터 평문 http 는 OS 가 막는다. 디버그든 릴리즈든 서버는 https 만 쓴다.
        assertTrue(
            "BASE_URL=${BuildConfig.BASE_URL}",
            BuildConfig.BASE_URL.startsWith("https://"),
        )
    }
}
