package com.example.moamap.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * 서버가 오래 붙잡고 있는 요청만 읽기 타임아웃을 늘린다.
 *
 * 기본 읽기 타임아웃은 15초인데, 인스타그램 장소 추출은 서버가 AI 분석을 돌려 최대 30초까지
 * 걸린다(로딩 화면 안내 문구도 그렇게 적혀 있다). 전역 타임아웃을 늘리면 다른 요청까지
 * 실패를 늦게 알게 되므로 이 경로만 따로 연장한다.
 */
class LongRunningTimeoutInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath !in LONG_RUNNING_PATHS) return chain.proceed(request)

        return chain
            .withReadTimeout(LONG_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .proceed(request)
    }

    private companion object {
        val LONG_RUNNING_PATHS = setOf(
            "/api/v1/places/instagram-extractions",
        )
        const val LONG_READ_TIMEOUT_SECONDS = 60
    }
}
