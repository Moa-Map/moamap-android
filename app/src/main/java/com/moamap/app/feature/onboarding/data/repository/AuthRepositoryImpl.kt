package com.moamap.app.feature.onboarding.data.repository

import android.content.Context
import com.moamap.app.core.auth.AgreedTermsVersionStore
import com.moamap.app.core.auth.AuthToken
import com.moamap.app.core.auth.AuthTokenStore
import com.moamap.app.core.auth.CurrentUserStore
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.onboarding.data.remote.AuthService
import com.moamap.app.feature.onboarding.data.remote.KakaoAuthClient
import com.moamap.app.feature.onboarding.data.remote.KakaoLoginRequestDto
import com.moamap.app.feature.onboarding.data.remote.LogoutRequestDto
import com.moamap.app.feature.onboarding.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val kakaoAuthClient: KakaoAuthClient,
    private val authService: AuthService,
    private val tokenStore: AuthTokenStore,
    private val currentUserStore: CurrentUserStore,
    private val agreedTermsVersionStore: AgreedTermsVersionStore,
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

        // 새 세션은 아직 약관에 동의하지 않았다. 토큰보다 먼저 지워야 동의 화면에서 앱을 꺼도 지난
        // 세션의 동의가 남지 않고, 다음에 앱을 켤 때 로그아웃돼 다시 동의를 받는다.
        agreedTermsVersionStore.clear()
        // 신원을 먼저 쓴다. 두 저장소가 각자 디스크에 써서 하나만 성공할 수 있는데,
        // hasSession() 이 토큰만 보므로 토큰 쓰기가 세션이 성립하는 지점이 된다. 순서를
        // 뒤집으면 신원 저장이 실패했을 때 토큰만 남아, 로그인된 채로 신원이 없는 상태가 된다.
        currentUserStore.save(response.userId)
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
            // 지울 때는 토큰이 먼저다. 저장과 반대 순서인데, 신원을 먼저 지웠다가 토큰 삭제가
            // 실패하면 로그인된 채로 신원만 없는 상태가 된다. 토큰을 먼저 지우면 남은 신원은
            // 이미 로그아웃된 상태의 값이라, 다음 로그인이 덮어쓸 때까지 읽히지 않는다.
            tokenStore.clear()
            // 토큰 저장소가 저장소 전체를 비우는 데 기대지 않는다. 그 구현이 토큰 키만 지우도록
            // 좁혀지면 신원만 살아남아, 다른 계정으로 로그인했을 때 남의 글이 내 것으로 보인다.
            currentUserStore.clear()
        }
    }

    /**
     * 순서는 서버가 정했다: 카카오 연결 끊기 → 서버 탈퇴 → 로컬 정리.
     *
     * 중간에 실패하면 로컬 세션을 남겨 다시 시도하게 한다. 카카오를 끊은 뒤 탈퇴가 실패했다면, 다시 시도할 때 카카오는
     * 「이미 끊김」으로 지나간다. 서버가 「없는 회원」(404)이라고 하면 앞선 시도에서 탈퇴는 됐는데 응답만 못 받은 것이라
     * 성공으로 본다 - 아니면 탈퇴한 세션에 갇힌다.
     */
    override suspend fun withdraw() {
        kakaoAuthClient.unlink()
        try {
            authService.withdraw()
        } catch (e: ApiException) {
            if (e.code != USER_NOT_FOUND_CODE) throw e
        }
        // 지우는 순서는 로그아웃과 같다(토큰 먼저).
        tokenStore.clear()
        currentUserStore.clear()
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

/** `[404] USER_003: 사용자를 찾을 수 없습니다.` 탈퇴한 회원도 이 코드다. */
private const val USER_NOT_FOUND_CODE = "USER_003"
