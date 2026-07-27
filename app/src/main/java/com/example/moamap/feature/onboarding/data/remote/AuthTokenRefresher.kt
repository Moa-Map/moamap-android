package com.example.moamap.feature.onboarding.data.remote

import com.example.moamap.core.auth.AuthToken
import com.example.moamap.core.auth.TokenRefresher
import com.example.moamap.core.network.di.TokenRefreshClient
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 갱신 전용 클라이언트로 토큰을 재발급받는다.
 *
 * 주입받는 [AuthService] 는 반드시 [TokenRefreshClient] 쪽이어야 한다. 인증이 붙은 기본
 * 클라이언트를 쓰면 갱신 요청이 다시 401 -> 갱신 경로를 타면서 순환한다.
 */
@Singleton
class AuthTokenRefresher @Inject constructor(
    @param:TokenRefreshClient private val authService: AuthService,
) : TokenRefresher {

    override suspend fun refresh(refreshToken: String): AuthToken? {
        val response = try {
            authService.refresh(RefreshRequestDto(refreshToken))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // 리프레시 토큰까지 만료됐거나 서버에 닿지 못했다. 어느 쪽이든 재로그인이 필요하다.
            return null
        }

        val accessToken = response.accessToken
        if (accessToken.isNullOrEmpty()) return null

        return AuthToken(
            accessToken = accessToken,
            // 서버가 리프레시 토큰을 회전시키는지 확인되지 않았다. 새 값이 오면 항상 덮어쓴다 -
            // 회전하지 않는 서버라면 같은 값을 다시 쓰는 것이라 어느 쪽이든 안전하다.
            refreshToken = response.refreshToken?.takeIf { it.isNotEmpty() } ?: refreshToken,
        )
    }
}
