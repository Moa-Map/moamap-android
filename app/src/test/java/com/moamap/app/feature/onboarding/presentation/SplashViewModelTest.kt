package com.moamap.app.feature.onboarding.presentation

import android.content.Context
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.FakeTermsRepository
import com.moamap.app.feature.terms.domain.model.TermsAgreementStatus
import com.moamap.app.feature.terms.testTerms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

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

    private class FakeAuthRepository(
        var hasSession: Boolean,
        private val logoutError: Exception? = null,
    ) : AuthRepository {
        var logoutCount = 0

        override suspend fun loginWithKakao(context: Context) = TODO("사용하지 않음")

        override suspend fun loginWithGoogle(context: Context) = TODO("사용하지 않음")

        override suspend fun logout() {
            logoutCount++
            logoutError?.let { throw it }
        }

        override suspend fun withdraw() = TODO("사용하지 않음")

        override suspend fun hasSession(): Boolean = hasSession
    }

    private fun status(needsAgreement: Boolean) = TermsAgreementStatus(
        needsAgreement = needsAgreement,
        pendingTerms = if (needsAgreement) listOf(testTerms(1, "SERVICE")) else emptyList(),
    )

    private fun viewModel(auth: AuthRepository, terms: FakeTermsRepository): SplashViewModel =
        SplashViewModel(auth, terms).also { dispatcher.scheduler.advanceUntilIdle() }

    @Test
    fun `세션이 없으면 동의 상태를 묻지 않고 로그인 화면으로 간다`() {
        val auth = FakeAuthRepository(hasSession = false)
        val terms = FakeTermsRepository()

        assertEquals(SplashDestination.Login, viewModel(auth, terms).destination.value)
        assertEquals(0, terms.statusCount)
        assertEquals(0, auth.logoutCount)
    }

    @Test
    fun `동의할 약관이 없으면 홈으로 간다`() {
        val auth = FakeAuthRepository(hasSession = true)
        val terms = FakeTermsRepository(statuses = listOf(status(needsAgreement = false)))

        assertEquals(SplashDestination.Main, viewModel(auth, terms).destination.value)
        assertEquals(0, auth.logoutCount)
    }

    @Test
    fun `동의할 약관이 남았으면 동의 화면으로 간다`() {
        val auth = FakeAuthRepository(hasSession = true)
        val terms = FakeTermsRepository(statuses = listOf(status(needsAgreement = true)))

        assertEquals(SplashDestination.TermsAgreement, viewModel(auth, terms).destination.value)
        assertEquals(0, auth.logoutCount)
    }

    /** 홈으로 그냥 보내지도, 로그아웃하지도 않는다. */
    @Test
    fun `동의 상태를 확인하지 못하면 스플래시에 머물러 다시 시도하게 한다`() {
        listOf(
            ConnectionException(IOException("offline")),
            ApiException(code = "COMMON_005", status = 503, serverMessage = "점검 중"),
        ).forEach { error ->
            val auth = FakeAuthRepository(hasSession = true)
            val terms = FakeTermsRepository().apply { statusError = error }

            val viewModel = viewModel(auth, terms)

            assertEquals(SplashDestination.Undecided, viewModel.destination.value)
            assertTrue(viewModel.termsCheckFailed.value)
            assertEquals(0, auth.logoutCount)
        }
    }

    @Test
    fun `다시 시도하면 동의 상태를 다시 묻고 결과대로 간다`() {
        val auth = FakeAuthRepository(hasSession = true)
        val terms = FakeTermsRepository(statuses = listOf(status(needsAgreement = true)))
            .apply { statusError = ConnectionException(IOException("offline")) }
        val viewModel = viewModel(auth, terms)

        terms.statusError = null
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, terms.statusCount)
        assertFalse(viewModel.termsCheckFailed.value)
        assertEquals(SplashDestination.TermsAgreement, viewModel.destination.value)
    }

    private val unauthorized = ApiException(code = "AUTH_001", status = 401, serverMessage = "만료")

    /** 서버가 토큰 갱신을 거부하면 `TokenAuthenticator` 가 세션을 지운 뒤 401 이 올라온다. */
    @Test
    fun `401 뒤 세션이 비었으면 로그아웃하고 로그인 화면으로 간다`() {
        val auth = FakeAuthRepository(hasSession = true)
        val terms = FakeTermsRepository().apply {
            statusError = unauthorized
            onAuthorizedCall = { auth.hasSession = false }
        }

        val viewModel = viewModel(auth, terms)

        assertEquals(SplashDestination.Login, viewModel.destination.value)
        assertFalse(viewModel.termsCheckFailed.value)
        assertEquals(1, auth.logoutCount)
    }

    @Test
    fun `401 뒤 로그아웃이 실패해도 로그인 화면으로 간다`() {
        val auth = FakeAuthRepository(hasSession = true, logoutError = RuntimeException("boom"))
        val terms = FakeTermsRepository().apply {
            statusError = unauthorized
            onAuthorizedCall = { auth.hasSession = false }
        }

        assertEquals(SplashDestination.Login, viewModel(auth, terms).destination.value)
    }

    /** 토큰 갱신이 네트워크 오류·5xx 로 잠깐 실패하면 토큰이 남은 채 401 이 올라온다. 로그아웃하면 서버 리프레시 토큰까지 버린다. */
    @Test
    fun `401 이어도 세션이 남아 있으면 로그아웃하지 않고 다시 시도하게 한다`() {
        val auth = FakeAuthRepository(hasSession = true)
        val terms = FakeTermsRepository().apply { statusError = unauthorized }

        val viewModel = viewModel(auth, terms)

        assertEquals(SplashDestination.Undecided, viewModel.destination.value)
        assertTrue(viewModel.termsCheckFailed.value)
        assertEquals(0, auth.logoutCount)
    }
}
