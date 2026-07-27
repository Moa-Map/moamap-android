package com.example.moamap.feature.onboarding.data.remote

import com.example.moamap.core.auth.AuthToken
import com.example.moamap.core.auth.TokenRefreshResult
import com.example.moamap.core.auth.TokenRefresher
import com.example.moamap.core.network.ApiException
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

    override suspend fun refresh(refreshToken: String): TokenRefreshResult {
        val response = try {
            authService.refresh(RefreshRequestDto(refreshToken))
        } catch (e: CancellationException) {
            throw e
        } catch (_: ApiException) {
            // 서버가 명시적으로 거부했다. 리프레시 토큰이 만료됐거나 폐기된 것이다.
            return TokenRefreshResult.Rejected
        } catch (_: Throwable) {
            // 연결 실패·타임아웃 등. 리프레시 토큰은 멀쩡할 수 있으므로 세션을 지우면 안 된다.
            return TokenRefreshResult.Failed
        }

        val accessToken = response.accessToken
        if (accessToken.isNullOrEmpty()) return TokenRefreshResult.Failed

        return TokenRefreshResult.Success(
            AuthToken(
                accessToken = accessToken,
                // 서버가 리프레시 토큰을 회전시키는지 확인되지 않았다. 새 값이 오면 항상 덮어쓴다 -
                // 회전하지 않는 서버라면 같은 값을 다시 쓰는 것이라 어느 쪽이든 안전하다.
                refreshToken = response.refreshToken?.takeIf { it.isNotEmpty() } ?: refreshToken,
            )
        )
    }
}
