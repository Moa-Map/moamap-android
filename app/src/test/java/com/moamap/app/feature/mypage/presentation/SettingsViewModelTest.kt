package com.moamap.app.feature.mypage.presentation

import android.content.Context
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import com.moamap.app.feature.terms.FakeTermsRepository
import com.moamap.app.feature.terms.domain.model.TermsKind
import com.moamap.app.feature.terms.testTerms
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

    override suspend fun loginWithGoogle(context: Context) = Unit

    override suspend fun logout() {
        logoutCount++
        logoutFailure?.let { throw it }
    }

    var withdrawFailure: Throwable? = null

    var withdrawCount: Int = 0
        private set

    override suspend fun withdraw() {
        withdrawCount++
        withdrawFailure?.let { throw it }
    }

    override suspend fun hasSession(): Boolean = false
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()
    private val terms = FakeTermsRepository(
        termsList = listOf(
            testTerms(11, "SERVICE"),
            testTerms(12, "AGE"),
            testTerms(13, "POLICY", TermsKind.VIEW_ONLY),
        ),
    )

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SettingsViewModel(repository, terms)
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

    // ---- 회원 탈퇴 (이슈 #320) ----

    @Test
    fun `탈퇴에 성공하면 로그아웃과 같이 이동 신호를 낸다`() = runTest(dispatcher) {
        viewModel.withdraw()
        advanceUntilIdle()

        assertTrue(viewModel.loggedOut.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `탈퇴에 실패하면 남아서 안내한다`() = runTest(dispatcher) {
        repository.withdrawFailure = ApiException(code = "COMMON_006", status = 500, serverMessage = "서버 내부 오류가 발생했습니다.")

        viewModel.withdraw()
        advanceUntilIdle()

        assertFalse(viewModel.loggedOut.value)
        assertEquals("탈퇴하지 못했어요. 잠시 후 다시 시도해 주세요.", viewModel.errorMessage.value)
    }

    /** 다른 요청과 겹친 409 는 서버가 다시 시도하라고 안내한다. 그 문구를 그대로 보여 준다. */
    @Test
    fun `겹친 요청이면 서버의 다시 시도 안내를 보여 준다`() = runTest(dispatcher) {
        repository.withdrawFailure = ApiException(
            code = "USER_010",
            status = 409,
            serverMessage = "다른 요청과 동시에 처리되어 반영하지 못했습니다. 다시 시도해 주세요.",
        )

        viewModel.withdraw()
        advanceUntilIdle()

        assertEquals("다른 요청과 동시에 처리되어 반영하지 못했습니다. 다시 시도해 주세요.", viewModel.errorMessage.value)
    }

    @Test
    fun `탈퇴 중에 다시 눌러도 한 번만 요청한다`() = runTest(dispatcher) {
        viewModel.withdraw()
        viewModel.withdraw()
        advanceUntilIdle()

        assertEquals(1, repository.withdrawCount)
    }

    // ---- 이용약관·개인정보처리방침 (이슈 #350) ----

    @Test
    fun `약관 목록에서 코드로 찾은 약관 번호로 전문을 연다`() = runTest(dispatcher) {
        viewModel.openTerms("SERVICE")
        advanceUntilIdle()
        assertEquals(11L, viewModel.termsToOpen.value)

        viewModel.consumeTermsToOpen()
        viewModel.openTerms("POLICY")
        advanceUntilIdle()
        assertEquals(13L, viewModel.termsToOpen.value)
    }

    @Test
    fun `약관 목록을 못 불러오면 안내를 띄운다`() = runTest(dispatcher) {
        terms.listError = ConnectionException(IOException("offline"))

        viewModel.openTerms("SERVICE")
        advanceUntilIdle()

        assertNull(viewModel.termsToOpen.value)
        assertEquals("네트워크에 연결할 수 없어요", viewModel.errorMessage.value)
    }

    @Test
    fun `목록에 그 약관이 없으면 안내를 띄운다`() = runTest(dispatcher) {
        terms.termsList = emptyList()

        viewModel.openTerms("POLICY")
        advanceUntilIdle()

        assertNull(viewModel.termsToOpen.value)
        assertEquals(TERMS_OPEN_ERROR, viewModel.errorMessage.value)
    }
}
