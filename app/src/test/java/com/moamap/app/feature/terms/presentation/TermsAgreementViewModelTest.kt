package com.moamap.app.feature.terms.presentation

import android.content.Context
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.FakeTermsRepository
import com.moamap.app.feature.terms.domain.model.Terms
import com.moamap.app.feature.terms.domain.model.TermsAgreementStatus
import com.moamap.app.feature.terms.domain.model.TermsKind
import com.moamap.app.feature.terms.testTerms
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

    private class FakeAuthRepository(var logoutError: Exception? = null) : AuthRepository {
        var logoutCount = 0

        override suspend fun loginWithKakao(context: Context) =
            throw UnsupportedOperationException("약관 화면은 로그인하지 않는다")

        override suspend fun loginWithGoogle(context: Context) =
            throw UnsupportedOperationException("약관 화면은 로그인하지 않는다")

        override suspend fun logout() {
            logoutCount++
            logoutError?.let { throw it }
        }

        override suspend fun withdraw() = TODO("사용하지 않음")

        override suspend fun hasSession(): Boolean = true
    }

    private val service = testTerms(1, "SERVICE")
    private val age = testTerms(2, "AGE")
    private val policy = testTerms(3, "POLICY", TermsKind.VIEW_ONLY)

    private fun pending(vararg terms: Terms) =
        TermsAgreementStatus(needsAgreement = true, pendingTerms = terms.toList())

    private fun repository(vararg statuses: TermsAgreementStatus = arrayOf(pending(service, age))) =
        FakeTermsRepository(termsList = listOf(service, age, policy), statuses = statuses.toList())

    private fun viewModel(
        terms: FakeTermsRepository = repository(),
        auth: AuthRepository = FakeAuthRepository(),
    ) = TermsAgreementViewModel(terms, auth).also {
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `체크박스 줄은 남은 약관만, 보기만 하는 약관은 따로 그린다`() {
        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertEquals(listOf(service, age), state.pendingTerms)
        assertEquals(listOf(policy), state.viewOnlyTerms)
        assertTrue(state.checked.isEmpty())
        assertFalse(state.requiredChecked)
    }

    /** 약관이 개정된 뒤면 바뀐 약관만 받는다. */
    @Test
    fun `개정된 약관만 남았으면 그것만 체크박스로 그린다`() {
        val state = viewModel(repository(pending(age))).uiState.value

        assertEquals(listOf(age), state.pendingTerms)
    }

    @Test
    fun `불러오지 못하면 안내를 띄우고 다시 시도하면 다시 부른다`() {
        val terms = repository().apply { statusError = ConnectionException(IOException("offline")) }
        val viewModel = viewModel(terms)
        assertEquals("네트워크에 연결할 수 없어요", viewModel.uiState.value.loadErrorMessage)

        terms.statusError = null
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadErrorMessage)
        assertEquals(listOf(service, age), viewModel.uiState.value.pendingTerms)
    }

    @Test
    fun `모두 동의하기 버튼은 전부 체크만 하고 동의를 보내지 않는다`() {
        val terms = repository()
        val viewModel = viewModel(terms)

        viewModel.onBottomButtonClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.requiredChecked)
        assertTrue(terms.agreedTermIds.isEmpty())
        assertNull(viewModel.uiState.value.result)
    }

    @Test
    fun `다음으로 버튼은 남은 약관 번호를 모두 보내고 성공하면 홈으로 간다`() {
        val terms = repository()
        val viewModel = viewModel(terms)
        viewModel.toggle(service.termId)
        viewModel.toggle(age.termId)

        viewModel.onBottomButtonClick()
        assertTrue(viewModel.uiState.value.submitting)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(listOf(1L, 2L)), terms.agreedTermIds)
        assertFalse(viewModel.uiState.value.submitting)
        assertEquals(TermsAgreementResult.Agreed, viewModel.uiState.value.result)
    }

    @Test
    fun `보내는 동안 다시 눌러도 한 번만 보낸다`() {
        val terms = repository()
        val viewModel = viewModel(terms)
        viewModel.toggleAll()

        viewModel.onBottomButtonClick()
        viewModel.onBottomButtonClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, terms.agreedTermIds.size)
    }

    @Test
    fun `409 면 동의 상태를 다시 불러와 새로 그리고 체크를 푼다`() {
        val revised = testTerms(4, "SERVICE")
        val terms = repository(pending(service, age), pending(revised)).apply {
            agreeError = ApiException(code = "USER_023", status = 409, serverMessage = "약관이 개정되었습니다.")
        }
        val viewModel = viewModel(terms)
        viewModel.toggleAll()

        viewModel.onBottomButtonClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, terms.statusCount)
        assertEquals(listOf(revised), state.pendingTerms)
        assertTrue(state.checked.isEmpty())
        assertEquals(TERMS_CHANGED_MESSAGE, state.message)
        assertNull(state.result)
    }

    @Test
    fun `그 밖의 실패면 화면에 머물고 서버 문구 대신 안내를 띄운다`() {
        val terms = repository().apply {
            agreeError = ApiException(code = "USER_022", status = 400, serverMessage = "필수 약관에 모두 동의해야 합니다.")
        }
        val viewModel = viewModel(terms)
        viewModel.toggleAll()

        viewModel.onBottomButtonClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TERMS_AGREE_FAILED_MESSAGE, state.message)
        assertFalse(state.submitting)
        assertTrue(state.requiredChecked)
        assertNull(state.result)

        viewModel.consumeMessage()
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `보낼 때 연결이 끊기면 네트워크 안내를 띄운다`() {
        val terms = repository().apply { agreeError = ConnectionException(IOException("offline")) }
        val viewModel = viewModel(terms)
        viewModel.toggleAll()

        viewModel.onBottomButtonClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("네트워크에 연결할 수 없어요", viewModel.uiState.value.message)
    }

    @Test
    fun `들어왔는데 이미 다 동의했으면 홈으로 간다`() {
        val viewModel = viewModel(repository(TermsAgreementStatus(needsAgreement = false, pendingTerms = emptyList())))

        assertEquals(TermsAgreementResult.Agreed, viewModel.uiState.value.result)
    }

    @Test
    fun `불러올 때 401 이면 로그아웃하고 로그인 화면으로 간다`() {
        val auth = FakeAuthRepository()
        val terms = repository().apply {
            statusError = ApiException(code = "AUTH_001", status = 401, serverMessage = "만료")
        }

        val viewModel = viewModel(terms, auth)

        assertEquals(1, auth.logoutCount)
        assertEquals(TermsAgreementResult.Cancelled, viewModel.uiState.value.result)
    }

    @Test
    fun `필수 하나를 풀면 다시 모두 동의하기 상태가 된다`() {
        val viewModel = viewModel()
        viewModel.onBottomButtonClick()

        viewModel.toggle(age.termId)

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
        val viewModel = viewModel(auth = auth)

        viewModel.cancel()
        viewModel.cancel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, auth.logoutCount)
        assertEquals(TermsAgreementResult.Cancelled, viewModel.uiState.value.result)
    }

    @Test
    fun `로그아웃이 실패해도 로그인 화면으로 돌아간다`() {
        val viewModel = viewModel(auth = FakeAuthRepository(logoutError = RuntimeException("boom")))

        viewModel.cancel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(TermsAgreementResult.Cancelled, viewModel.uiState.value.result)
    }

    @Test
    fun `결과를 읽으면 지운다`() {
        val viewModel = viewModel()
        viewModel.cancel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.consumeResult()

        assertNull(viewModel.uiState.value.result)
    }
}
