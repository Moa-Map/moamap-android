package com.moamap.app.feature.onboarding.data.remote

import com.moamap.app.core.auth.AuthToken
import com.moamap.app.core.auth.TokenRefreshResult
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class AuthTokenRefresherTest {

    @Test
    fun `서버가 401로 거부하면 거부로 본다`() = runTest {
        val result = refresherFailingWith(ApiException(code = "USER_002", status = 401, serverMessage = "만료"))
            .refresh("old")

        assertEquals(TokenRefreshResult.Rejected, result)
    }

    /** 배포·재시작 중 게이트웨이가 주는 오류다. 거부로 보면 그때마다 모두 로그아웃된다. */
    @Test
    fun `서버 일시 오류는 일시 실패로 본다`() = runTest {
        listOf(500, 503, 504).forEach { status ->
            val result = refresherFailingWith(ApiException(code = "COMMON_008", status = status, serverMessage = "점검"))
                .refresh("old")

            assertEquals("HTTP $status", TokenRefreshResult.Failed, result)
        }
    }

    @Test
    fun `서버에 닿지 못하면 일시 실패로 본다`() = runTest {
        val result = refresherFailingWith(ConnectionException(IOException("timeout"))).refresh("old")

        assertEquals(TokenRefreshResult.Failed, result)
    }

    @Test
    fun `새로 받은 토큰을 돌려준다`() = runTest {
        val refresher = AuthTokenRefresher(FakeRefreshAuthService { TokenDto(accessToken = "a2", refreshToken = "r2") })

        assertEquals(TokenRefreshResult.Success(AuthToken("a2", "r2")), refresher.refresh("old"))
    }

    private fun refresherFailingWith(error: Exception) =
        AuthTokenRefresher(FakeRefreshAuthService { throw error })
}

private class FakeRefreshAuthService(private val onRefresh: () -> TokenDto) : AuthService {

    override suspend fun kakaoLogin(request: KakaoLoginRequestDto): TokenDto = error("쓰지 않음")

    override suspend fun refresh(request: RefreshRequestDto): TokenDto = onRefresh()

    override suspend fun logout(request: LogoutRequestDto) = error("쓰지 않음")

    override suspend fun withdraw() = error("쓰지 않음")
}
