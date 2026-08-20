package com.moamap.app.core.auth

/**
 * 세션 토큰을 기기에 보관한다.
 *
 * OkHttp 인터셉터는 동기 API 라 `suspend` 함수를 호출할 수 없으므로,
 * 요청 경로에서 쓰는 [blockingAccessToken] 을 따로 둔다.
 */
interface AuthTokenStore {

    /**
     * 인터셉터처럼 동기 컨텍스트에서 액세스 토큰을 읽는다.
     *
     * 메모리 캐시를 먼저 보고, 프로세스 재시작 직후처럼 캐시가 비어 있을 때만 디스크를 한 번 읽는다.
     * 매 요청마다 디스크 I/O 가 요청 경로에 끼어들지 않게 하기 위한 구조다.
     */
    fun blockingAccessToken(): String?

    /** 저장된 토큰. 둘 중 하나라도 비어 있으면 세션이 없는 것으로 본다. */
    suspend fun load(): AuthToken?

    suspend fun save(token: AuthToken)

    suspend fun clear()
}
