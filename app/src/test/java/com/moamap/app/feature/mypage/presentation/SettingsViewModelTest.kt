package com.moamap.app.feature.mypage.presentation

import android.content.Context
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

private class FakeAuthRepository : AuthRepository {

    var logoutFailure: Throwable? = null

    var logoutCount: Int = 0
        private set

    override suspend fun loginWithKakao(context: Context) = Unit

    override suspend fun logout() {
        logoutCount++
        logoutFailure?.let { throw it }
    }

    override suspend fun hasSession(): Boolean = false
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `로그아웃에 성공하면 이동 신호를 낸다`() = runTest(dispatcher) {
        viewModel.logout()
        advanceUntilIdle()

        assertTrue(viewModel.loggedOut.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `로컬 세션 정리에 실패하면 로그아웃됐다고 알리지 않는다`() = runTest(dispatcher) {
        // 세션이 남았는데 로그인 화면으로 보내면, 앱을 다시 켰을 때 로그인 상태로 들어가 사용자를 속인다.
        repository.logoutFailure = IOException("저장소 정리 실패")

        viewModel.logout()
        advanceUntilIdle()

        assertFalse(viewModel.loggedOut.value)
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `에러를 소비하면 메시지가 사라진다`() = runTest(dispatcher) {
        repository.logoutFailure = IOException("저장소 정리 실패")
        viewModel.logout()
        advanceUntilIdle()

        viewModel.consumeError()

        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `이미 로그아웃했으면 다시 요청하지 않는다`() = runTest(dispatcher) {
        viewModel.logout()
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(1, repository.logoutCount)
    }
}
