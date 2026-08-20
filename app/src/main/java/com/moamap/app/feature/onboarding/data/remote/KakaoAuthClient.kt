package com.moamap.app.feature.onboarding.data.remote

import android.content.Context
import com.moamap.app.feature.onboarding.domain.model.KakaoLoginCancelledException
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 카카오 SDK 를 감싼다.
 *
 * SDK 는 콜백 기반이고 `UserApiClient.instance` 라는 정적 싱글톤이라, 그대로 쓰면 ViewModel 을
 * 테스트할 수 없다. 이 인터페이스를 경계로 두고 테스트에서는 가짜 구현으로 바꿔 끼운다.
 */
interface KakaoAuthClient {

    /**
     * 카카오 로그인 후 카카오 액세스 토큰을 돌려준다.
     *
     * @param context 로그인 화면을 띄우기 위한 Activity 컨텍스트
     * @throws KakaoLoginCancelledException 사용자가 취소한 경우
     */
    suspend fun login(context: Context): String

    suspend fun logout()
}

@Singleton
class KakaoAuthClientImpl @Inject constructor() : KakaoAuthClient {

    override suspend fun login(context: Context): String {
        val token = if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            loginWithKakaoTalkOrFallback(context)
        } else {
            loginWithKakaoAccount(context)
        }
        return token.accessToken
    }

    /**
     * 카카오톡 로그인을 먼저 시도하고 실패하면 카카오계정 로그인으로 넘어간다.
     *
     * 단, 사용자가 직접 취소한 경우에는 폴백하지 않는다. 취소했는데 곧바로 웹 로그인 창이
     * 다시 뜨면 사용자는 로그인을 그만둘 방법이 없다.
     */
    private suspend fun loginWithKakaoTalkOrFallback(context: Context): OAuthToken =
        try {
            suspendCancellableCoroutine { continuation ->
                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                    continuation.resumeWithKakaoResult(token, error)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: KakaoLoginCancelledException) {
            throw e
        } catch (_: Throwable) {
            loginWithKakaoAccount(context)
        }

    private suspend fun loginWithKakaoAccount(context: Context): OAuthToken =
        suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                continuation.resumeWithKakaoResult(token, error)
            }
        }

    override suspend fun logout() {
        suspendCancellableCoroutine<Unit> { continuation ->
            UserApiClient.instance.logout { error ->
                if (!continuation.isActive) return@logout
                if (error != null) {
                    continuation.resumeWithException(error)
                } else {
                    continuation.resume(Unit)
                }
            }
        }
    }
}

/** 카카오 SDK 의 (token, error) 콜백을 코루틴 결과로 옮긴다. */
private fun CancellableContinuation<OAuthToken>.resumeWithKakaoResult(
    token: OAuthToken?,
    error: Throwable?,
) {
    if (!isActive) return
    when {
        error != null -> resumeWithException(error.toDomainThrowable())
        token != null -> resume(token)
        else -> resumeWithException(IllegalStateException("카카오 로그인 결과가 비어 있습니다."))
    }
}

/** 사용자 취소는 실패가 아니라 선택이므로 도메인 예외로 바꿔 위로 올린다. */
private fun Throwable.toDomainThrowable(): Throwable =
    if (this is ClientError && reason == ClientErrorCause.Cancelled) {
        KakaoLoginCancelledException()
    } else {
        this
    }
