package com.moamap.app.feature.onboarding.data.remote

import com.kakao.sdk.common.model.ApiError
import com.kakao.sdk.common.model.ApiErrorCause
import com.kakao.sdk.common.model.ApiErrorResponse
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** 회원 탈퇴 전 카카오 연결 끊기(이슈 #320). 이미 끊긴 경우를 실패로 보면 탈퇴를 영영 못 한다. */
class KakaoAuthClientTest {

    @Test
    fun `카카오 로그인 정보가 없거나 통하지 않으면 이미 끊긴 것으로 본다`() {
        assertTrue(ClientError(ClientErrorCause.TokenNotFound).meansKakaoAlreadyUnlinked())
        assertTrue(apiError(ApiErrorCause.InvalidToken).meansKakaoAlreadyUnlinked())
        assertTrue(apiError(ApiErrorCause.NotRegisteredUser).meansKakaoAlreadyUnlinked())
    }

    /** 끊지 못한 채 탈퇴하면 카카오 쪽 연결이 남는다. 일시 오류는 멈추고 다시 시도하게 한다. */
    @Test
    fun `네트워크나 카카오 서버 오류는 끊긴 것으로 보지 않는다`() {
        assertFalse(IOException("offline").meansKakaoAlreadyUnlinked())
        assertFalse(apiError(ApiErrorCause.InternalError).meansKakaoAlreadyUnlinked())
        assertFalse(ClientError(ClientErrorCause.Cancelled).meansKakaoAlreadyUnlinked())
    }

    private fun apiError(cause: ApiErrorCause) = ApiError(-1, cause, ApiErrorResponse(-1, "kakao"))
}
