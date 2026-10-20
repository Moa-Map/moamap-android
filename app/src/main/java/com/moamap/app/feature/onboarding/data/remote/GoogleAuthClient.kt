package com.moamap.app.feature.onboarding.data.remote

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.moamap.app.BuildConfig
import com.moamap.app.feature.onboarding.domain.model.GoogleLoginCancelledException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credential Manager 의 구글 로그인을 감싼다.
 *
 * [KakaoAuthClient] 와 같은 이유로 인터페이스를 경계로 둔다 - 테스트에서는 가짜 구현으로 바꿔 끼운다.
 */
interface GoogleAuthClient {

    /**
     * 구글 계정을 고르게 한 뒤 구글 ID 토큰을 돌려준다.
     *
     * @param context 계정 선택 화면을 띄우기 위한 Activity 컨텍스트
     * @throws GoogleLoginCancelledException 사용자가 취소한 경우
     */
    suspend fun login(context: Context): String
}

@Singleton
class GoogleAuthClientImpl @Inject constructor() : GoogleAuthClient {

    override suspend fun login(context: Context): String {
        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        check(serverClientId.isNotEmpty()) { "GOOGLE_WEB_CLIENT_ID 가 비어 있습니다." }

        // 「구글로 계속하기」 버튼 흐름이다. 기기에 계정이 없으면 계정 추가 화면까지 이어 준다.
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()
        val response = try {
            CredentialManager.create(context).getCredential(context, request)
        } catch (_: GetCredentialCancellationException) {
            // 사용자 취소는 실패가 아니라 선택이므로 도메인 예외로 바꿔 위로 올린다.
            throw GoogleLoginCancelledException()
        }

        val credential = response.credential
        check(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
        ) { "구글 로그인 결과가 ID 토큰이 아닙니다." }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}

