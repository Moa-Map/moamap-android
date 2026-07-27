package com.example.moamap.feature.onboarding.data.repository

import android.content.Context
import com.example.moamap.core.auth.AuthToken
import com.example.moamap.core.auth.AuthTokenStore
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
) : AuthRepository {

    override suspend fun loginWithKakao(context: Context) {
        val kakaoAccessToken = kakaoAuthClient.login(context)
        val response = authService.kakaoLogin(KakaoLoginRequestDto(kakaoAccessToken))

        val accessToken = response.accessToken
        val refreshToken = response.refreshToken
        check(!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
            "로그인 응답에 토큰이 없습니다."
        }

        tokenStore.save(AuthToken(accessToken = accessToken, refreshToken = refreshToken))
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
