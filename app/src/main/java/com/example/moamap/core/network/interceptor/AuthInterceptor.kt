package com.example.moamap.core.network.interceptor

import com.example.moamap.core.auth.AuthTokenStore
import okhttp3.Interceptor
import okhttp3.Response

/** 인증 헤더 이름과 접두사. Authenticator 도 같은 값을 써야 해서 밖으로 뺀다. */
internal const val AUTHORIZATION_HEADER = "Authorization"
internal const val BEARER_PREFIX = "Bearer "

/**
 * 토큰이 필요 없는 경로.
 *
 * 특히 갱신 요청에 만료된 액세스 토큰을 실으면 갱신 자체가 401 로 막힌다.
 */
private val NO_AUTH_PATHS = setOf(
    "/api/v1/auth/kakao/login",
    "/api/v1/auth/token/refresh",
)

/**
 * 저장된 액세스 토큰을 요청 헤더에 싣는다.
 *
 * 토큰이 없으면 헤더 없이 그대로 보낸다. 서버가 401 을 내려주고 그게 정상 흐름이다.
 * [ErrorInterceptor] 보다 **뒤에** 등록해야 한다 - ErrorInterceptor 가 가장 바깥에서
 * 실패 응답을 예외로 정규화하는 기존 구조를 유지한다.
 */
class AuthInterceptor(
    private val tokenStore: AuthTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath in NO_AUTH_PATHS) return chain.proceed(request)

        val accessToken = tokenStore.blockingAccessToken() ?: return chain.proceed(request)

        return chain.proceed(
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                .build()
        )
    }
}
