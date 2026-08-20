package com.moamap.app.core.network.authenticator

import com.moamap.app.core.auth.AuthTokenStore
import com.moamap.app.core.auth.CurrentUserStore
import com.moamap.app.core.auth.TokenRefreshResult
import com.moamap.app.core.auth.TokenRefresher
import com.moamap.app.core.network.interceptor.AUTHORIZATION_HEADER
import com.moamap.app.core.network.interceptor.BEARER_PREFIX
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 401 응답을 받으면 토큰을 한 번 갱신하고 원래 요청을 재시도한다.
 *
 * OkHttp 는 Authenticator 를 `RetryAndFollowUpInterceptor` 안에서 호출하는데, 이 지점은
 * application interceptor 인 `ErrorInterceptor` 보다 **아래**다. 덕분에 갱신 재시도가 먼저 일어나고
 * 끝내 실패한 응답만 ErrorInterceptor 가 `ApiException` 으로 바꾼다.
 *
 * 갱신 호출은 인증 인터셉터가 붙지 않은 별도 클라이언트를 쓰는 [TokenRefresher] 로 나가야 한다.
 * 같은 클라이언트를 쓰면 갱신 요청이 다시 인증 경로를 타면서 순환한다.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: AuthTokenStore,
    private val currentUserStore: CurrentUserStore,
    private val tokenRefresher: Provider<TokenRefresher>,
) : Authenticator {

    /** 여러 요청이 동시에 401 을 받아도 갱신은 한 번만 나가게 한다. */
    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        val failedToken = response.request.header(AUTHORIZATION_HEADER)
            ?.removePrefix(BEARER_PREFIX)
            // 애초에 토큰을 싣지 않은 요청이면 갱신해도 소용없다.
            ?: return null

        // 갱신한 토큰으로도 401 이면 더 시도하지 않는다. 없으면 무한 루프가 된다.
        if (responseCount(response) >= MAX_ATTEMPTS) return null

        val newAccessToken = runBlocking {
            refreshMutex.withLock {
                val current = tokenStore.load() ?: return@withLock null

                // 락을 기다리는 사이 다른 요청이 이미 갱신했다면 다시 갱신하지 않고 새 토큰으로 재시도만 한다.
                if (current.accessToken != failedToken) return@withLock current.accessToken

                when (val result = tokenRefresher.get().refresh(current.refreshToken)) {
                    is TokenRefreshResult.Success -> {
                        tokenStore.save(result.token)
                        // 신원은 건드리지 않는다. 갱신으로 사람이 바뀌지는 않는다.
                        result.token.accessToken
                    }

                    // 서버가 거부했다. 남겨두면 매 요청마다 갱신을 재시도하게 된다.
                    TokenRefreshResult.Rejected -> {
                        tokenStore.clear()
                        // 세션이 끝났으니 신원도 함께 버린다. 남겨두면 다음 사람이 이 기기에
                        // 로그인했을 때 남의 글이 자기 것으로 보인다.
                        currentUserStore.clear()
                        null
                    }

                    // 네트워크 장애 같은 일시적 실패. 세션을 지우면 통신이 잠깐 끊겼다는 이유로
                    // 재로그인을 강요하게 되므로, 이번 요청만 실패시키고 토큰은 남겨둔다.
                    TokenRefreshResult.Failed -> null
                }
            }
        } ?: return null

        return response.request.newBuilder()
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + newAccessToken)
            .build()
    }

    /** 이 응답이 몇 번째 시도인지. priorResponse 체인을 거슬러 센다. */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        /** 최초 요청 1회 + 갱신 후 재시도 1회. */
        const val MAX_ATTEMPTS = 2
    }
}
