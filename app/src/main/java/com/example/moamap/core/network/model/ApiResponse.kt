package com.example.moamap.core.network.model

import kotlinx.serialization.Serializable

/**
 * 서버가 모든 응답을 감싸는 공통 envelope.
 *
 * 성공: {"success": true, "data": <페이로드>, "error": null}
 * 본문 없는 성공: {"success": true}
 * 실패: {"success": false, "error": {...}}
 *
 * 이 타입은 [com.example.moamap.core.network.EnvelopeConverterFactory] 안에서만 쓴다.
 * 각 feature 의 Service 는 envelope 을 모른 채 페이로드 타입만 선언한다.
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: ErrorResponse? = null,
)

/** 실패 응답의 error 필드. 게이트웨이 404/503/타임아웃도 같은 형태로 내려온다. */
@Serializable
data class ErrorResponse(
    val code: String = UNKNOWN_ERROR_CODE,
    val message: String = "",
    val status: Int = 0,
)

/** 서버가 코드를 주지 않았거나 본문을 파싱하지 못했을 때 쓰는 값. */
const val UNKNOWN_ERROR_CODE: String = "UNKNOWN"
