package com.moamap.app.core.auth

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 세션이 서버 쪽에서 끝났음을 화면에 알린다.
 *
 * 세션을 지우는 곳(네트워크 계층)과 로그인 화면으로 보내는 곳(내비게이션)이 서로를 모르게 이
 * 클래스를 사이에 둔다. 사용자가 직접 로그아웃한 경우는 화면이 이미 알고 있어 여기로 보내지 않는다.
 */
@Singleton
class SessionEvents @Inject constructor() {

    /**
     * 받는 쪽이 없을 때 보낸 신호도 남겨 둔다.
     *
     * 화면이 수집을 시작하기 전에 세션이 끝나도 놓치지 않는다. 동시에 여러 요청이 거부돼도 이동은
     * 한 번이면 되므로 하나로 합친다.
     */
    private val expired = Channel<Unit>(Channel.CONFLATED)

    val sessionExpired: Flow<Unit> = expired.receiveAsFlow()

    /** OkHttp 스레드에서 부른다. 막히지 않는다. */
    fun notifySessionExpired() {
        expired.trySend(Unit)
    }
}
