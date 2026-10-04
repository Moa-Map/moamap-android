package com.moamap.app.feature.terms.presentation

import android.content.Context
import com.moamap.app.core.auth.FakeAgreedTermsVersionStore
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.domain.model.Terms
import com.moamap.app.feature.terms.domain.model.TermsConsentType
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TermsAgreementViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun terms(code: String, type: TermsConsentType) =
        Terms(code = code, title = code, consentType = type, version = "1", content = "")

    private class FakeTermsRepository(private val terms: List<Terms>) : TermsRepository {
        override suspend fun getAgreementTerms(): List<Terms> = terms
        override suspend fun getTerms(code: String): Terms = terms.first { it.code == code }
        override suspend fun getCurrentVersion(): String = "v2"
    }

    private class FakeAuthRepository(var logoutError: Exception? = null) : AuthRepository {
        var logoutCount = 0

        override suspend fun loginWithKakao(context: Context) =
            throw UnsupportedOperationException("약관 화면은 로그인하지 않는다")

        override suspend fun logout() {
            logoutCount++
            logoutError?.let { throw it }
        }

        override suspend fun hasSession(): Boolean = true
    }

    private val required1 = terms("SERVICE", TermsConsentType.REQUIRED)
    private val required2 = terms("PRIVACY", TermsConsentType.REQUIRED)
    private val optional = terms("MARKETING", TermsConsentType.OPTIONAL)

    private fun viewModel(
        auth: AuthRepository = FakeAuthRepository(),
        termsVersionStore: FakeAgreedTermsVersionStore = FakeAgreedTermsVersionStore(),
    ) = TermsAgreementViewModel(
        FakeTermsRepository(listOf(required1, required2, optional)),
        auth,
        termsVersionStore,
    ).also {
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `처음에는 아무것도 체크돼 있지 않고 버튼은 모두 동의하기다`() {
        val state = viewModel().uiState.value

        assertEquals(3, state.terms.size)
        assertTrue(state.checked.isEmpty())
        assertFalse(state.requiredChecked)
        assertFalse(state.allChecked)
    }

    @Test
    fun `모두 동의하기 버튼은 전부 체크만 하고 로그인을 마치지 않는다`() {
        val viewModel = viewModel()

        viewModel.onBottomButtonClick()

        val state = viewModel.uiState.value
        assertTrue(state.allChecked)
        assertTrue(state.requiredChecked)
        assertNull(state.result)
    }

    @Test
    fun `필수만 체크해도 다음으로 버튼이 지금 약관 버전을 남기고 로그인을 마친다`() {
        val termsVersionStore = FakeAgreedTermsVersionStore()
        val viewModel = viewModel(termsVersionStore = termsVersionStore)
        viewModel.toggle(required1.code)
        viewModel.toggle(required2.code)
        assertTrue(viewModel.uiState.value.requiredChecked)
        assertFalse(viewModel.uiState.value.allChecked)

        viewModel.onBottomButtonClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("v2", termsVersionStore.version)
        assertEquals(TermsAgreementResult.Agreed, viewModel.uiState.value.result)
    }

    /** 다음에 앱을 켤 때 로그아웃돼 다시 동의할 뿐이라 여기서 막지 않는다. */
    @Test
    fun `동의한 약관 버전을 못 남겨도 로그인을 마친다`() {
        val termsVersionStore = FakeAgreedTermsVersionStore().apply { saveError = IOException("boom") }
        val viewModel = viewModel(termsVersionStore = termsVersionStore)
        viewModel.toggleAll()

        viewModel.onBottomButtonClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(termsVersionStore.version)
        assertEquals(TermsAgreementResult.Agreed, viewModel.uiState.value.result)
    }

    @Test
    fun `필수 하나를 풀면 다시 모두 동의하기 상태가 된다`() {
        val viewModel = viewModel()
        viewModel.onBottomButtonClick()

        viewModel.toggle(required2.code)

        assertFalse(viewModel.uiState.value.requiredChecked)
        assertFalse(viewModel.uiState.value.allChecked)
    }

    @Test
    fun `모두 동의하기 줄은 전부 체크하고 다시 누르면 전부 푼다`() {
        val viewModel = viewModel()

        viewModel.toggleAll()
        assertTrue(viewModel.uiState.value.allChecked)

        viewModel.toggleAll()
        assertTrue(viewModel.uiState.value.checked.isEmpty())
    }

    @Test
    fun `뒤로 가면 로그아웃하고 취소로 끝난다 - 연타해도 한 번만`() {
        val auth = FakeAuthRepository()
        val viewModel = viewModel(auth)

        viewModel.cancel()
        viewModel.cancel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, auth.logoutCount)
        assertEquals(TermsAgreementResult.Cancelled, viewModel.uiState.value.result)
    }

    @Test
    fun `로그아웃이 실패해도 로그인 화면으로 돌아간다`() {
        val viewModel = viewModel(FakeAuthRepository(logoutError = RuntimeException("boom")))

        viewModel.cancel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(TermsAgreementResult.Cancelled, viewModel.uiState.value.result)
    }

    @Test
    fun `결과를 읽으면 지운다`() {
        val viewModel = viewModel()
        viewModel.onBottomButtonClick()
        viewModel.onBottomButtonClick()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(TermsAgreementResult.Agreed, viewModel.uiState.value.result)

        viewModel.consumeResult()

        assertNull(viewModel.uiState.value.result)
    }
}
