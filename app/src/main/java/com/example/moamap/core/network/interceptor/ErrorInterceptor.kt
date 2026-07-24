package com.example.moamap.core.network.interceptor

import com.example.moamap.core.network.ApiException
import com.example.moamap.core.network.ConnectionException
import com.example.moamap.core.network.model.ErrorResponse
import com.example.moamap.core.network.model.UNKNOWN_ERROR_CODE
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** 실패 응답 본문에서 error 만 떼어 읽기 위한 최소 형태. */
@Serializable
private data class ErrorEnvelope(val error: ErrorResponse? = null)

/**
 * 실패를 한 곳에서 예외로 정규화한다.
 *
 * - 서버에 닿지 못함 → [ConnectionException]
 * - non-2xx 응답 → [ApiException] (서버 코드/메시지 보존, 파싱 실패 시 HTTP 상태로 대체)
 *
 * **application interceptor 로 등록해야 한다.** network interceptor 로 넣으면
 * 리다이렉트·재시도마다 호출되고, 다음 이슈에서 붙일 Authenticator 기반 토큰 갱신보다
 * 아래쪽에 놓여 갱신 재시도를 막는다.
 */
class ErrorInterceptor(
    private val json: Json,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = try {
            chain.proceed(chain.request())
        } catch (e: IOException) {
            throw ConnectionException(e)
        }

        if (response.isSuccessful) return response

        // 예외를 던지면 호출부가 응답을 닫을 수 없으므로 여기서 본문을 읽고 직접 닫는다.
        val rawBody = response.body?.string().orEmpty()
        response.close()

        val error = runCatching {
            json.decodeFromString(ErrorEnvelope.serializer(), rawBody).error
        }.getOrNull()

        throw ApiException(
            code = error?.code ?: UNKNOWN_ERROR_CODE,
            status = error?.status?.takeIf { it != 0 } ?: response.code,
            serverMessage = error?.message?.takeIf { it.isNotBlank() } ?: response.message,
        )
    }
}
