package com.moamap.app.core.network.interceptor

import com.moamap.app.core.auth.AuthTokenStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Interceptor
import okhttp3.Response
import okio.ByteString.Companion.decodeBase64

/** 인증 헤더 이름과 접두사. Authenticator 도 같은 값을 써야 해서 밖으로 뺀다. */
internal const val AUTHORIZATION_HEADER = "Authorization"
internal const val BEARER_PREFIX = "Bearer "

/**
 * 토큰이 필요 없는 경로.
 *
 * 특히 갱신 요청에 만료된 액세스 토큰을 실으면 갱신 자체가 401 로 막힌다. 로그아웃도 토큰을 싣지 않는다 - 실으면
 * 보내기 직전 미리 새로 받기가 리프레시 토큰을 회전시켜, 로그아웃이 이미 지워진 옛 토큰만 지우고 새 토큰은
 * 서버에 남는다. 서버는 `/api/v1/auth` 아래를 로그인 없이 받는다.
 */
private val NO_AUTH_PATHS = setOf(
    "/api/v1/auth/kakao/login",
    "/api/v1/auth/google/login",
    "/api/v1/auth/token/refresh",
    "/api/v1/auth/logout",
)

/**
 * 이 헤더가 붙은 요청은 토큰 없이 보낸다. 헤더는 떼고 보낸다.
 *
 * 경로가 아니라 요청 하나를 고르는 표시다. 같은 경로라도 로그인 없이 보내야 하는 요청이 있다 - 커뮤니티 지도
 * 전체보기(`GET /maps`)는 로그인하면 서버가 참여한 지도를 빼고 주지만, 지도 만들기(`POST /maps`)는 로그인이 필요하다.
 */
internal const val ANONYMOUS_REQUEST_HEADER = "X-MoaMap-Anonymous"

/** 만료까지 이만큼 남았으면 보내기 전에 새로 받는다. 보내는 사이에 만료되지 않게 둔 여유다. */
private const val REFRESH_MARGIN_SECONDS = 60L

/**
 * 저장된 액세스 토큰을 요청 헤더에 싣는다.
 *
 * 토큰이 없으면 헤더 없이 그대로 보낸다. 서버가 401 을 내려주고 그게 정상 흐름이다.
 * [ErrorInterceptor] 보다 **뒤에** 등록해야 한다 - ErrorInterceptor 가 가장 바깥에서
 * 실패 응답을 예외로 정규화하는 기존 구조를 유지한다.
 *
 * 싣기 전에 토큰 안의 만료 시각(`exp`)을 보고, 만료됐거나 곧 만료되면 [refreshToken] 으로 먼저 새로 받는다.
 * 서버는 지도 조회(`GET /api/v1/maps` 아래 전부)처럼 로그인 없이도 되는 경로에 만료된 토큰이 오면 401 대신 비로그인으로
 * 처리해서, 401 때만 갱신하면 오래 쉬었다 연 홈·지도가 「참여 안 함」으로 보인다(이슈 #302). 받지 못하면
 * 원래 토큰으로 보낸다 - 서버가 401 을 주면 Authenticator 가 한 번 더 해 본다.
 *
 * @param refreshToken 오래된 액세스 토큰을 받아 새 토큰을 돌려준다. 401 때와 같은 갱신(`TokenAuthenticator.refresh`)이다.
 * @param nowSeconds 지금 시각(초). 테스트가 바꿔 끼운다.
 */
class AuthInterceptor(
    private val tokenStore: AuthTokenStore,
    private val refreshToken: (staleAccessToken: String) -> String? = { null },
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) : Interceptor {

    /**
     * 막 받은 토큰도 만료로 보이면 기기 시계가 틀린 것이다. 그대로 두면 요청마다 새로 받으므로, 이번 실행 동안은
     * 미리 받지 않고 401 때만 받는다.
     */
    @Volatile
    private var refreshAheadDisabled = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath in NO_AUTH_PATHS) return chain.proceed(request)
        if (request.header(ANONYMOUS_REQUEST_HEADER) != null) {
            return chain.proceed(request.newBuilder().removeHeader(ANONYMOUS_REQUEST_HEADER).build())
        }

        var accessToken = tokenStore.blockingAccessToken() ?: return chain.proceed(request)
        if (!refreshAheadDisabled && isExpiring(accessToken)) {
            refreshToken(accessToken)?.let { refreshed ->
                if (isExpiring(refreshed)) refreshAheadDisabled = true
                accessToken = refreshed
            }
        }

        return chain.proceed(
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                .build()
        )
    }

    /** 만료 시각을 못 읽는 토큰은 지금처럼 401 때만 갱신한다. */
    private fun isExpiring(accessToken: String): Boolean {
        val expiresAt = jwtExpiresAtSeconds(accessToken) ?: return false
        return nowSeconds() >= expiresAt - REFRESH_MARGIN_SECONDS
    }
}

/**
 * JWT 의 만료 시각(`exp`, 초). JWT 가 아니거나 읽지 못하면 null 이다.
 *
 * 서명은 확인하지 않는다 - 서버가 확인하고, 여기서는 언제 새로 받을지만 정한다. 서버 응답의 `expiresIn`(남은 초)
 * 대신 토큰에서 읽어, 이미 로그인해 있던 사람에게도 저장 형식을 바꾸지 않고 바로 적용된다.
 */
internal fun jwtExpiresAtSeconds(token: String): Long? {
    val payload = token.split('.').takeIf { parts -> parts.size == 3 }?.get(1) ?: return null
    // okio 는 URL-safe 글자와 패딩 없는 입력을 모두 받는다. java.util.Base64 는 API 26 부터라 못 쓴다.
    val json = payload.decodeBase64()?.utf8() ?: return null
    return runCatching { Json.parseToJsonElement(json).jsonObject["exp"]?.jsonPrimitive?.longOrNull }.getOrNull()
}
