package com.moamap.app.feature.onboarding.data.remote

import com.moamap.app.core.auth.AuthToken
import com.moamap.app.core.auth.TokenRefreshResult
import com.moamap.app.core.auth.TokenRefresher
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.di.TokenRefreshClient
import kotlinx.coroutines.CancellationException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
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
        } catch (e: ApiException) {
            // 서버가 거부하는 경우(토큰·사용자 없음)는 401 하나뿐이다. 배포 중 503 같은 나머지는
            // 일시적인 실패라, 거부로 보면 서버가 잠깐 멈출 때마다 모두 로그아웃된다.
            return if (e.status == HTTP_UNAUTHORIZED) TokenRefreshResult.Rejected else TokenRefreshResult.Failed
        } catch (_: Throwable) {
            // 연결 실패·타임아웃 등. 리프레시 토큰은 멀쩡할 수 있으므로 세션을 지우면 안 된다.
            return TokenRefreshResult.Failed
        }

        val accessToken = response.accessToken
        if (accessToken.isNullOrEmpty()) return TokenRefreshResult.Failed

        return TokenRefreshResult.Success(
            AuthToken(
                accessToken = accessToken,
                // 서버는 갱신할 때마다 리프레시 토큰을 새로 발급하고 옛것을 지운다(회전). 새 값을 꼭 저장해야 한다.
                refreshToken = response.refreshToken?.takeIf { it.isNotEmpty() } ?: refreshToken,
            )
        )
    }
}
