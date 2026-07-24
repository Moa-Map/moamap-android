package com.example.moamap.core.network

import java.io.IOException

/**
 * 네트워크 계층이 도메인으로 던지는 예외.
 */
sealed class NetworkException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/**
 * 서버가 실패 응답을 내려준 경우.
 *
 * @param code 서버 에러 코드(예: COMMON_005). 분기는 message 문자열이 아니라 이 값으로 한다.
 * @param status HTTP 상태 코드
 * @param serverMessage 서버가 준 사용자 노출용 메시지
 */
class ApiException(
    val code: String,
    val status: Int,
    val serverMessage: String,
) : NetworkException("[$status] $code: $serverMessage")

/** 서버에 닿지 못한 경우(연결 실패, 타임아웃, DNS 실패 등). */
class ConnectionException(
    cause: Throwable,
) : NetworkException("네트워크에 연결할 수 없습니다.", cause)
