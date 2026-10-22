package com.moamap.app.core.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreLoginScreenTest {

    @Test
    fun `스플래시·로그인·약관 동의는 로그인 전 화면이다`() {
        listOf(MoaMapRoute.Splash, MoaMapRoute.Login, MoaMapRoute.TermsAgreement).forEach { route ->
            assertTrue(isPreLoginScreen(route.route, termsAgreementInBackStack = false))
        }
    }

    /** 동의 전에 공유 링크를 처리하거나 세션 만료로 옮기면 안 된다. */
    @Test
    fun `동의 화면 위에서 연 약관 전문은 로그인 전 화면이다`() {
        assertTrue(isPreLoginScreen(MoaMapRoute.TermsDetail.route, termsAgreementInBackStack = true))
    }

    @Test
    fun `설정에서 연 약관 전문은 로그인 후 화면이다`() {
        assertFalse(isPreLoginScreen(MoaMapRoute.TermsDetail.route, termsAgreementInBackStack = false))
    }

    @Test
    fun `로그인 후 화면은 아래에 무엇이 있든 로그인 후다`() {
        assertFalse(isPreLoginScreen(MoaMapRoute.Explore.route, termsAgreementInBackStack = true))
        assertFalse(isPreLoginScreen(MoaMapRoute.Settings.route, termsAgreementInBackStack = false))
    }
}
