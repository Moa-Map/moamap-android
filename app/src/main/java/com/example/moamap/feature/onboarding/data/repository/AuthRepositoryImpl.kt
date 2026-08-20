package com.example.moamap.feature.onboarding.data.repository

import android.content.Context
import com.example.moamap.core.auth.AuthToken
import com.example.moamap.core.auth.AuthTokenStore
import com.example.moamap.core.auth.CurrentUserStore
import com.example.moamap.feature.onboarding.data.remote.AuthService
import com.example.moamap.feature.onboarding.data.remote.KakaoAuthClient
import com.example.moamap.feature.onboarding.data.remote.KakaoLoginRequestDto
import com.example.moamap.feature.onboarding.data.remote.LogoutRequestDto
import com.example.moamap.feature.onboarding.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val kakaoAuthClient: KakaoAuthClient,
    private val authService: AuthService,
    private val tokenStore: AuthTokenStore,
    private val currentUserStore: CurrentUserStore,
) : AuthRepository {

    /**
     * 토큰과 식별자 중 하나라도 빠지면 세션을 열지 않는다.
     *
     * 신원을 모른 채 들어가면 후기 목록에서 내 것과 남의 것을 가릴 수 없다. 수정·삭제가 붙어야
     * 할 자리에 신고가 붙거나 그 반대가 되는데, 화면이 조용히 어긋나느니 여기서 멈추는 편이 낫다.
     */
    override suspend fun loginWithKakao(context: Context) {
        val kakaoAccessToken = kakaoAuthClient.login(context)
        val response = authService.kakaoLogin(KakaoLoginRequestDto(kakaoAccessToken))

        val accessToken = response.accessToken
        val refreshToken = response.refreshToken
        check(!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
            "로그인 응답에 토큰이 없습니다."
        }
        check(response.userId > 0) { "로그인 응답에 사용자 식별자가 없습니다." }

        tokenStore.save(AuthToken(accessToken = accessToken, refreshToken = refreshToken))
        currentUserStore.save(response.userId)
    }

    /**
     * 서버와 카카오 로그아웃을 시도하되, 실패해도 로컬 세션은 반드시 지운다.
     *
     * 네트워크가 끊긴 상태에서 로그아웃이 막히면 사용자가 앱에서 빠져나갈 방법이 없어진다.
     * 다만 **로컬 삭제 실패는 삼키지 않는다** - 세션이 남은 채로 로그아웃됐다고 알리면
     * 앱을 다시 켰을 때 로그인 상태로 들어가 사용자를 속이게 된다.
     */
    override suspend fun logout() {
        try {
            val refreshToken = tokenStore.load()?.refreshToken

            if (refreshToken != null) {
                runIgnoringFailure { authService.logout(LogoutRequestDto(refreshToken)) }
            }
            runIgnoringFailure { kakaoAuthClient.logout() }
        } finally {
            tokenStore.clear()
            // 토큰 저장소가 저장소 전체를 비우는 데 기대지 않는다. 그 구현이 토큰 키만 지우도록
            // 좁혀지면 신원만 살아남아, 다른 계정으로 로그인했을 때 남의 글이 내 것으로 보인다.
            currentUserStore.clear()
        }
    }

    override suspend fun hasSession(): Boolean = tokenStore.load() != null

    private suspend inline fun runIgnoringFailure(block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // 로컬 정리를 막지 않는다.
        }
    }
}
