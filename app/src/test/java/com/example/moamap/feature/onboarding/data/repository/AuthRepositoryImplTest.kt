package com.example.moamap.feature.onboarding.data.repository

import android.content.Context
import android.content.ContextWrapper
import com.example.moamap.core.auth.AuthToken
import com.example.moamap.core.auth.FakeAuthTokenStore
import com.example.moamap.core.auth.FakeCurrentUserStore
import com.example.moamap.feature.onboarding.data.remote.AuthService
import com.example.moamap.feature.onboarding.data.remote.KakaoAuthClient
import com.example.moamap.feature.onboarding.data.remote.KakaoLoginRequestDto
import com.example.moamap.feature.onboarding.data.remote.LogoutRequestDto
import com.example.moamap.feature.onboarding.data.remote.RefreshRequestDto
import com.example.moamap.feature.onboarding.data.remote.TokenDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class FakeKakaoAuthClient : KakaoAuthClient {
    var logoutCount: Int = 0
        private set

    override suspend fun login(context: Context): String = "kakao-access"

    override suspend fun logout() {
        logoutCount++
    }
}

private class FakeAuthService(
    private val loginResponse: TokenDto = TokenDto(
        userId = 42,
        accessToken = "a",
        refreshToken = "r",
    ),
) : AuthService {

    var logoutCount: Int = 0
        private set

    override suspend fun kakaoLogin(request: KakaoLoginRequestDto): TokenDto = loginResponse

    override suspend fun refresh(request: RefreshRequestDto): TokenDto = TODO("사용하지 않음")

    override suspend fun logout(request: LogoutRequestDto) {
        logoutCount++
    }
}

class AuthRepositoryImplTest {

    private val context: Context = ContextWrapper(null)

    private fun repository(
        authService: AuthService = FakeAuthService(),
        tokenStore: FakeAuthTokenStore = FakeAuthTokenStore(),
        userStore: FakeCurrentUserStore = FakeCurrentUserStore(),
    ) = AuthRepositoryImpl(
        kakaoAuthClient = FakeKakaoAuthClient(),
        authService = authService,
        tokenStore = tokenStore,
        currentUserStore = userStore,
    )

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
        }

        repository(authService = failing, tokenStore = tokenStore, userStore = userStore).logout()

        assertNull(userStore.userId)
    }
}
