package com.moamap.app.feature.onboarding.presentation

import android.content.Context
import com.moamap.app.core.auth.FakeAgreedTermsVersionStore
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.domain.model.Terms
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val CURRENT_VERSION = "2026-10-09"

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeTermsRepository : TermsRepository {
        override suspend fun getAgreementTerms(): List<Terms> = emptyList()
        override suspend fun getTerms(code: String): Terms = TODO("사용하지 않음")
        override suspend fun getCurrentVersion(): String = CURRENT_VERSION
    }

    private class FakeAuthRepository(
        private val hasSession: Boolean,
        private val logoutError: Exception? = null,
    ) : AuthRepository {
        var logoutCount = 0

        override suspend fun loginWithKakao(context: Context) = TODO("사용하지 않음")

        override suspend fun logout() {
            logoutCount++
            logoutError?.let { throw it }
        }

        override suspend fun hasSession(): Boolean = hasSession
    }

    private fun destination(auth: AuthRepository, agreedVersion: String?): SplashDestination {
        val viewModel = SplashViewModel(auth, FakeTermsRepository(), FakeAgreedTermsVersionStore(agreedVersion))
        dispatcher.scheduler.advanceUntilIdle()
        return viewModel.destination.value
    }

    @Test
    fun `세션이 없으면 로그인 화면으로 간다`() {
        val auth = FakeAuthRepository(hasSession = false)

        assertEquals(SplashDestination.Login, destination(auth, agreedVersion = null))
        assertEquals(0, auth.logoutCount)
    }

    @Test
    fun `지금 약관에 동의한 세션이면 홈으로 간다`() {
        val auth = FakeAuthRepository(hasSession = true)

        assertEquals(SplashDestination.Main, destination(auth, agreedVersion = CURRENT_VERSION))
        assertEquals(0, auth.logoutCount)
    }

    /** 이 기능 전부터 로그인해 있던 사람, 동의 화면에서 앱을 끈 사람, 약관이 바뀐 뒤 처음 켠 사람. */
    @Test
    fun `동의한 버전이 없거나 다르면 로그아웃하고 로그인 화면으로 간다`() {
        listOf(null, "2026-01-01").forEach { agreedVersion ->
            val auth = FakeAuthRepository(hasSession = true)

            assertEquals(SplashDestination.Login, destination(auth, agreedVersion))
            assertEquals(1, auth.logoutCount)
        }
    }

    @Test
    fun `로그아웃이 실패해도 로그인 화면으로 간다`() {
        val auth = FakeAuthRepository(hasSession = true, logoutError = RuntimeException("boom"))

        assertEquals(SplashDestination.Login, destination(auth, agreedVersion = null))
    }
}
