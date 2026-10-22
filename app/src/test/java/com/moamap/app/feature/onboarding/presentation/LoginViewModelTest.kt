package com.moamap.app.feature.onboarding.presentation

import android.content.Context
import android.content.ContextWrapper
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.onboarding.domain.model.GoogleLoginCancelledException
import com.moamap.app.feature.onboarding.domain.model.KakaoLoginCancelledException
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.FakeTermsRepository
import com.moamap.app.feature.terms.domain.model.TermsAgreementStatus
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import com.moamap.app.feature.terms.testTerms
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

private class FakeAuthRepository : AuthRepository {

    /** null 이면 즉시 성공한다. 값을 넣으면 완료될 때까지 로그인이 매달린다. */
    var pending: CompletableDeferred<Unit>? = null

    var failure: Throwable? = null

    var loginCount: Int = 0
        private set

    var googleLoginCount: Int = 0
        private set

    override suspend fun loginWithKakao(context: Context) {
        loginCount++
        pending?.await()
        failure?.let { throw it }
    }

    override suspend fun loginWithGoogle(context: Context) {
        googleLoginCount++
        pending?.await()
        failure?.let { throw it }
    }

    var logoutCount: Int = 0
        private set

    var logoutFailure: Throwable? = null

    /** null 이면 즉시 끝난다. 값을 넣으면 완료될 때까지 로그아웃이 매달린다. */
    var pendingLogout: CompletableDeferred<Unit>? = null

    override suspend fun logout() {
        logoutCount++
        pendingLogout?.await()
        logoutFailure?.let { throw it }
    }

    override suspend fun withdraw() = TODO("사용하지 않음")

    /** 로그인에 성공하면 세션이 생긴다. */
    var session: Boolean = true

    override suspend fun hasSession(): Boolean = session
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()
    private val terms = FakeTermsRepository()

    /** 카카오 SDK 때문에 시그니처에 남아 있을 뿐 이 테스트에서는 쓰이지 않는다. */
    private val context: Context = ContextWrapper(null)

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = LoginViewModel(repository, terms)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `로그인에 성공하면 이동 신호를 낸다`() = runTest(dispatcher) {
        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        assertEquals(LoginUiState.Success(needsTermsAgreement = false), viewModel.uiState.value)
    }

    @Test
    fun `이동 신호를 소비하면 초기 상태로 돌아간다`() = runTest(dispatcher) {
        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        viewModel.consumeSuccess()

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `사용자가 취소하면 에러 없이 초기 상태로 돌아간다`() = runTest(dispatcher) {
        repository.failure = KakaoLoginCancelledException()

        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `서버가 준 메시지를 그대로 노출한다`() = runTest(dispatcher) {
        repository.failure = ApiException(
            code = "AUTH_001",
            status = 400,
            serverMessage = "카카오 토큰이 유효하지 않습니다.",
        )

        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        assertEquals(
            LoginUiState.Error("카카오 토큰이 유효하지 않습니다."),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `서버 내부 오류는 서버 문구 대신 기본 안내를 노출한다`() = runTest(dispatcher) {
        repository.failure = ApiException(
            code = "COMMON_006",
            status = 500,
            serverMessage = "서버 내부 오류가 발생했습니다.",
        )

        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        assertEquals(LoginUiState.Error("카카오 로그인에 실패했어요"), viewModel.uiState.value)
    }

    @Test
    fun `서버에 닿지 못하면 네트워크 메시지를 노출한다`() = runTest(dispatcher) {
        repository.failure = ConnectionException(IOException("boom"))

        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        assertEquals(
            LoginUiState.Error("네트워크에 연결할 수 없어요"),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `로그인이 진행 중이면 버튼을 다시 눌러도 무시한다`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.pending = gate

        viewModel.loginWithKakao(context)
        advanceUntilIdle()
        assertEquals(LoginUiState.Loading, viewModel.uiState.value)

        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        assertEquals(1, repository.loginCount)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    // ---- 구글 로그인 (이슈 #332) ----

    @Test
    fun `구글 로그인에 성공하면 카카오와 같은 이동 신호를 낸다`() = runTest(dispatcher) {
        viewModel.loginWithGoogle(context)
        advanceUntilIdle()

        assertEquals(1, repository.googleLoginCount)
        assertEquals(LoginUiState.Success(needsTermsAgreement = false), viewModel.uiState.value)
    }

    @Test
    fun `구글 계정 선택을 취소하면 에러 없이 초기 상태로 돌아간다`() = runTest(dispatcher) {
        repository.failure = GoogleLoginCancelledException()

        viewModel.loginWithGoogle(context)
        advanceUntilIdle()

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `구글 로그인 실패는 구글 기본 안내를 노출한다`() = runTest(dispatcher) {
        repository.failure = IllegalStateException("GOOGLE_WEB_CLIENT_ID 가 비어 있습니다.")

        viewModel.loginWithGoogle(context)
        advanceUntilIdle()

        assertEquals(LoginUiState.Error("구글 로그인에 실패했어요"), viewModel.uiState.value)
    }

    /** 두 계정 창이 겹쳐 뜨지 않게 한다. 화면도 로딩 중에는 두 버튼을 모두 막는다. */
    @Test
    fun `카카오 로그인이 진행 중이면 구글 로그인을 시작하지 않는다`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.pending = gate

        viewModel.loginWithKakao(context)
        advanceUntilIdle()
        viewModel.loginWithGoogle(context)
        advanceUntilIdle()

        assertEquals(0, repository.googleLoginCount)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    // ---- 로그인 직후 약관 동의 상태 확인 (이슈 #350) ----

    @Test
    fun `동의할 약관이 남았으면 동의 화면으로 가라는 신호를 낸다`() = runTest(dispatcher) {
        val needs = FakeTermsRepository(
            statuses = listOf(TermsAgreementStatus(needsAgreement = true, pendingTerms = listOf(testTerms(1, "SERVICE")))),
        )
        viewModel = LoginViewModel(repository, needs)

        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        assertEquals(LoginUiState.Success(needsTermsAgreement = true), viewModel.uiState.value)
    }

    @Test
    fun `동의 상태를 확인하는 동안 로딩을 이어 간다`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val slow = object : TermsRepository by terms {
            override suspend fun getAgreementStatus(): TermsAgreementStatus {
                gate.await()
                return terms.getAgreementStatus()
            }
        }
        viewModel = LoginViewModel(repository, slow)

        viewModel.loginWithKakao(context)
        advanceUntilIdle()
        assertEquals(LoginUiState.Loading, viewModel.uiState.value)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(LoginUiState.Success(needsTermsAgreement = false), viewModel.uiState.value)
    }

    /** 홈으로 그냥 보내지도, 로그아웃하지도 않는다. */
    @Test
    fun `동의 상태를 확인하지 못하면 안내를 띄우고 다시 시도하면 다시 묻는다`() = runTest(dispatcher) {
        terms.statusError = ConnectionException(IOException("offline"))

        viewModel.loginWithKakao(context)
        advanceUntilIdle()
        assertEquals(LoginUiState.TermsCheckFailed(), viewModel.uiState.value)
        assertEquals(0, repository.logoutCount)

        terms.statusError = null
        viewModel.retryTermsCheck()
        advanceUntilIdle()

        assertEquals(1, repository.loginCount)
        assertEquals(2, terms.statusCount)
        assertEquals(LoginUiState.Success(needsTermsAgreement = false), viewModel.uiState.value)
    }

    @Test
    fun `동의 상태 확인이 401 이고 세션이 비었으면 세션을 지우고 로그인 화면에 머문다`() = runTest(dispatcher) {
        terms.statusError = ApiException(code = "AUTH_001", status = 401, serverMessage = "만료")
        terms.onAuthorizedCall = { repository.session = false }

        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        assertEquals(1, repository.logoutCount)
        assertEquals(LoginUiState.Error("카카오 로그인에 실패했어요"), viewModel.uiState.value)
    }

    /** 토큰 갱신이 잠깐 실패한 것이다. 로그아웃하면 방금 받은 서버 리프레시 토큰까지 버린다. */
    @Test
    fun `동의 상태 확인이 401 이어도 세션이 남아 있으면 로그아웃하지 않고 다시 시도하게 한다`() = runTest(dispatcher) {
        terms.statusError = ApiException(code = "AUTH_001", status = 401, serverMessage = "만료")

        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        assertEquals(0, repository.logoutCount)
        assertEquals(LoginUiState.TermsCheckFailed(), viewModel.uiState.value)
    }

    @Test
    fun `로그인이 실패하면 동의 상태를 묻지 않는다`() = runTest(dispatcher) {
        repository.failure = ConnectionException(IOException("boom"))

        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        assertEquals(0, terms.statusCount)
    }

    // ---- 확인 실패 중 로그아웃·로그인 버튼 (iOS 에 맞춤) ----

    private fun TestScope.failTermsCheck() {
        terms.statusError = ConnectionException(IOException("offline"))
        viewModel.loginWithKakao(context)
        advanceUntilIdle()
        assertEquals(LoginUiState.TermsCheckFailed(), viewModel.uiState.value)
    }

    /** 세션이 열려 있어 「로그아웃」으로만 빠져나간다. */
    @Test
    fun `약관 확인에 실패한 동안에는 로그인 버튼이 먹지 않는다`() = runTest(dispatcher) {
        failTermsCheck()

        viewModel.loginWithKakao(context)
        viewModel.loginWithGoogle(context)
        advanceUntilIdle()

        assertEquals(1, repository.loginCount)
        assertEquals(0, repository.googleLoginCount)
        assertEquals(LoginUiState.TermsCheckFailed(), viewModel.uiState.value)
    }

    @Test
    fun `로그아웃하면 세션을 지우고 처음 로그인 화면으로 돌아간다`() = runTest(dispatcher) {
        failTermsCheck()

        viewModel.logoutAfterTermsCheckFailed()
        advanceUntilIdle()

        assertEquals(1, repository.logoutCount)
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `로그아웃 중에는 다시 시도와 로그아웃을 다시 누를 수 없다`() = runTest(dispatcher) {
        failTermsCheck()
        val gate = CompletableDeferred<Unit>()
        repository.pendingLogout = gate

        viewModel.logoutAfterTermsCheckFailed()
        advanceUntilIdle()
        assertEquals(LoginUiState.TermsCheckFailed(loggingOut = true), viewModel.uiState.value)

        viewModel.logoutAfterTermsCheckFailed()
        viewModel.retryTermsCheck()
        advanceUntilIdle()

        assertEquals(1, repository.logoutCount)
        assertEquals(1, terms.statusCount)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `로그아웃에 실패하면 안내를 띄우고 확인 실패 화면에 머문다`() = runTest(dispatcher) {
        failTermsCheck()
        repository.logoutFailure = IOException("disk")

        viewModel.logoutAfterTermsCheckFailed()
        advanceUntilIdle()

        assertEquals(
            LoginUiState.TermsCheckFailed(logoutErrorMessage = "로그아웃하지 못했어요. 잠시 후 다시 시도해 주세요."),
            viewModel.uiState.value,
        )

        viewModel.consumeError()
        assertEquals(LoginUiState.TermsCheckFailed(), viewModel.uiState.value)
    }
}
