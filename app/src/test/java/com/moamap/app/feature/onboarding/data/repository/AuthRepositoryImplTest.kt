package com.moamap.app.feature.onboarding.data.repository

import android.content.Context
import android.content.ContextWrapper
import com.moamap.app.core.auth.AuthToken
import com.moamap.app.core.auth.FakeAgreedTermsVersionStore
import com.moamap.app.core.auth.FakeAuthTokenStore
import com.moamap.app.core.auth.FakeCurrentUserStore
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.onboarding.data.remote.AuthService
import com.moamap.app.feature.onboarding.data.remote.KakaoAuthClient
import com.moamap.app.feature.onboarding.data.remote.KakaoLoginRequestDto
import com.moamap.app.feature.onboarding.data.remote.LogoutRequestDto
import com.moamap.app.feature.onboarding.data.remote.RefreshRequestDto
import com.moamap.app.feature.onboarding.data.remote.TokenDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class FakeKakaoAuthClient(
    private val calls: MutableList<String> = mutableListOf(),
    private val unlinkError: Throwable? = null,
) : KakaoAuthClient {
    var logoutCount: Int = 0
        private set

    override suspend fun login(context: Context): String = "kakao-access"

    override suspend fun logout() {
        logoutCount++
    }

    override suspend fun unlink() {
        calls += "kakao-unlink"
        unlinkError?.let { throw it }
    }
}

private class FakeAuthService(
    private val loginResponse: TokenDto = TokenDto(
        userId = 42,
        accessToken = "a",
        refreshToken = "r",
    ),
    private val calls: MutableList<String> = mutableListOf(),
    private val withdrawError: Throwable? = null,
) : AuthService {

    var logoutCount: Int = 0
        private set

    override suspend fun kakaoLogin(request: KakaoLoginRequestDto): TokenDto = loginResponse

    override suspend fun refresh(request: RefreshRequestDto): TokenDto = TODO("사용하지 않음")

    override suspend fun logout(request: LogoutRequestDto) {
        logoutCount++
    }

    override suspend fun withdraw() {
        calls += "withdraw"
        withdrawError?.let { throw it }
    }
}

class AuthRepositoryImplTest {

    private val context: Context = ContextWrapper(null)

    private fun repository(
        authService: AuthService = FakeAuthService(),
        tokenStore: FakeAuthTokenStore = FakeAuthTokenStore(),
        userStore: FakeCurrentUserStore = FakeCurrentUserStore(),
        termsVersionStore: FakeAgreedTermsVersionStore = FakeAgreedTermsVersionStore(),
        kakaoAuthClient: KakaoAuthClient = FakeKakaoAuthClient(),
    ) = AuthRepositoryImpl(
        kakaoAuthClient = kakaoAuthClient,
        authService = authService,
        tokenStore = tokenStore,
        currentUserStore = userStore,
        agreedTermsVersionStore = termsVersionStore,
    )

    /** 지난 세션의 동의가 남으면 동의 화면에서 앱을 꺼도 다음 실행에 동의 없이 들어간다. */
    @Test
    fun `로그인하면 지난 세션의 약관 동의 버전을 지운다`() = runTest {
        val termsVersionStore = FakeAgreedTermsVersionStore("2026-10-09")

        repository(termsVersionStore = termsVersionStore).loginWithKakao(context)

        assertNull(termsVersionStore.version)
    }

    @Test
    fun `로그인하면 토큰과 함께 사용자 식별자를 저장한다`() = runTest {
        val tokenStore = FakeAuthTokenStore()
        val userStore = FakeCurrentUserStore()

        repository(tokenStore = tokenStore, userStore = userStore).loginWithKakao(context)

        assertEquals("a", tokenStore.token?.accessToken)
        assertEquals(42L, userStore.userId)
    }

    /**
     * 토큰 쓰기가 세션이 성립하는 지점이다.
     *
     * 두 저장소가 각자 디스크에 쓰므로 하나만 성공할 수 있다. 토큰을 먼저 쓰면 신원 저장이
     * 실패했을 때 토큰만 남아, [AuthRepository.hasSession] 이 참인데 신원이 없는 상태가 된다.
     * 신원을 먼저 써 두면 그 경우 세션 자체가 열리지 않아 사용자가 다시 로그인하고, 그때 두
     * 값이 함께 새로 쓰인다.
     */
    @Test
    fun `신원을 저장하지 못하면 토큰도 남기지 않는다`() = runTest {
        val tokenStore = FakeAuthTokenStore()
        val userStore = FakeCurrentUserStore().apply { saveError = IOException("boom") }

        runCatching {
            repository(tokenStore = tokenStore, userStore = userStore).loginWithKakao(context)
        }

        assertNull(tokenStore.token)
    }

    /**
     * 신원 없이 세션을 열면 후기 목록에서 내 것과 남의 것을 가릴 수 없다. 화면이 조용히
     * 어긋나느니 로그인에서 멈춘다. 토큰이 빠진 응답을 다루는 방식과 같다.
     */
    @Test
    fun `식별자가 없으면 로그인에 실패한다`() = runTest {
        val tokenStore = FakeAuthTokenStore()
        val userStore = FakeCurrentUserStore()
        val service = FakeAuthService(TokenDto(accessToken = "a", refreshToken = "r"))

        val error = runCatching {
            repository(authService = service, tokenStore = tokenStore, userStore = userStore)
                .loginWithKakao(context)
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertNull(userStore.userId)
    }

    /**
     * 남겨두면 다른 계정으로 로그인했을 때 남의 글이 내 것으로 보인다.
     * 토큰 저장소가 저장소 전체를 비우는 데 기대지 않고 여기서 함께 지운다.
     */
    @Test
    fun `로그아웃하면 식별자도 지운다`() = runTest {
        val tokenStore = FakeAuthTokenStore(AuthToken("a", "r"))
        val userStore = FakeCurrentUserStore(42L)

        repository(tokenStore = tokenStore, userStore = userStore).logout()

        assertNull(tokenStore.token)
        assertNull(userStore.userId)
    }

    /** 통신이 끊겨 서버 로그아웃이 실패해도 기기에 남은 신원은 지워야 한다. */
    @Test
    fun `서버 로그아웃이 실패해도 식별자를 지운다`() = runTest {
        val tokenStore = FakeAuthTokenStore(AuthToken("a", "r"))
        val userStore = FakeCurrentUserStore(42L)
        val failing = object : AuthService {
            override suspend fun kakaoLogin(request: KakaoLoginRequestDto) = TODO("사용하지 않음")
            override suspend fun refresh(request: RefreshRequestDto) = TODO("사용하지 않음")
            override suspend fun logout(request: LogoutRequestDto): Unit =
                throw RuntimeException("boom")
            override suspend fun withdraw() = TODO("사용하지 않음")
        }

        repository(authService = failing, tokenStore = tokenStore, userStore = userStore).logout()

        assertNull(userStore.userId)
    }

    // ---- 회원 탈퇴 (이슈 #320) ----

    /** 서버가 정한 순서다. 카카오 연결은 서버가 끊을 수 없어 앱이 탈퇴 전에 끊는다. */
    @Test
    fun `탈퇴는 카카오 연결을 끊고 서버에서 탈퇴한 뒤 세션을 지운다`() = runTest {
        val calls = mutableListOf<String>()
        val tokenStore = FakeAuthTokenStore(AuthToken("a", "r"))
        val userStore = FakeCurrentUserStore(42L)

        repository(
            authService = FakeAuthService(calls = calls),
            tokenStore = tokenStore,
            userStore = userStore,
            kakaoAuthClient = FakeKakaoAuthClient(calls = calls),
        ).withdraw()

        assertEquals(listOf("kakao-unlink", "withdraw"), calls)
        assertNull(tokenStore.token)
        assertNull(userStore.userId)
    }

    /** 끊지 못한 채 탈퇴하면 카카오 쪽 연결이 남는다. 세션을 남겨 다시 시도하게 한다. */
    @Test
    fun `카카오 연결을 끊지 못하면 탈퇴하지 않고 세션을 남긴다`() = runTest {
        val calls = mutableListOf<String>()
        val tokenStore = FakeAuthTokenStore(AuthToken("a", "r"))

        val error = runCatching {
            repository(
                authService = FakeAuthService(calls = calls),
                tokenStore = tokenStore,
                kakaoAuthClient = FakeKakaoAuthClient(calls = calls, unlinkError = IOException("offline")),
            ).withdraw()
        }.exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals(listOf("kakao-unlink"), calls)
        assertEquals("a", tokenStore.token?.accessToken)
    }

    @Test
    fun `서버 탈퇴가 실패하면 세션을 남긴다`() = runTest {
        val tokenStore = FakeAuthTokenStore(AuthToken("a", "r"))
        val failure = ApiException(code = "COMMON_006", status = 500, serverMessage = "서버 내부 오류가 발생했습니다.")

        val error = runCatching {
            repository(authService = FakeAuthService(withdrawError = failure), tokenStore = tokenStore).withdraw()
        }.exceptionOrNull()

        assertEquals(failure, error)
        assertEquals("a", tokenStore.token?.accessToken)
    }

    /** 앞선 시도에서 탈퇴는 됐는데 응답만 못 받은 경우다. 실패로 두면 탈퇴한 세션에 갇힌다. */
    @Test
    fun `이미 없는 회원이라는 답은 탈퇴 성공으로 본다`() = runTest {
        val tokenStore = FakeAuthTokenStore(AuthToken("a", "r"))
        val gone = ApiException(code = "USER_003", status = 404, serverMessage = "사용자를 찾을 수 없습니다.")

        repository(authService = FakeAuthService(withdrawError = gone), tokenStore = tokenStore).withdraw()

        assertNull(tokenStore.token)
    }
}
