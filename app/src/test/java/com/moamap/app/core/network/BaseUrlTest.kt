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
    fun `BASE_URL은 스킴을 포함한 절대 주소다`() {
        assertTrue(
            "BASE_URL=${BuildConfig.BASE_URL}",
            BuildConfig.BASE_URL.startsWith("http://") || BuildConfig.BASE_URL.startsWith("https://"),
        )
    }
}
