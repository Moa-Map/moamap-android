package com.example.moamap.feature.onboarding.presentation

import android.content.Context
import android.content.ContextWrapper
import com.example.moamap.core.network.ApiException
import com.example.moamap.core.network.ConnectionException
import com.example.moamap.feature.onboarding.domain.model.KakaoLoginCancelledException
import com.example.moamap.feature.onboarding.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

    override suspend fun loginWithKakao(context: Context) {
        loginCount++
        pending?.await()
        failure?.let { throw it }
    }

    override suspend fun logout() = Unit

    override suspend fun hasSession(): Boolean = false
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()

    /** 카카오 SDK 때문에 시그니처에 남아 있을 뿐 이 테스트에서는 쓰이지 않는다. */
    private val context: Context = ContextWrapper(null)

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = LoginViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `로그인에 성공하면 이동 신호를 낸다`() = runTest(dispatcher) {
        viewModel.loginWithKakao(context)
        advanceUntilIdle()

        assertEquals(LoginUiState.Success, viewModel.uiState.value)
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
}
