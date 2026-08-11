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
 *
 * 외부 지도 추출도 같은 부류다. 서버가 단축링크 리다이렉트를 따라가고(최대 10초), 지도
 * 서비스의 리스트 API 를 부른 뒤(최대 10초), 장소마다 카카오 재매칭까지 돌린다. 구글
 * 리스트처럼 항목이 많으면 15초를 쉽게 넘겨 앱만 타임아웃으로 실패한다.
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
            "/api/v1/places/map-share-extractions",
        )
        const val LONG_READ_TIMEOUT_SECONDS = 60
    }
}
