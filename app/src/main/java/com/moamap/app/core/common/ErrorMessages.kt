package com.moamap.app.core.common

import com.moamap.app.core.common.upload.ImageUploadException
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.core.network.model.UNKNOWN_ERROR_CODE

// 실패 안내 문구. 화면마다 따로 두던 것을 한곳에 모았다.

internal const val NETWORK_ERROR_MESSAGE = "네트워크에 연결할 수 없어요"
internal const val PHOTO_UPLOAD_FAILED_MESSAGE = "사진을 올리지 못했어요"

/**
 * 실패 안내 문구.
 *
 * 서버 메시지를 그대로 노출하지 않는다. `ApiException` 은 `[500] COMMON_005: ...` 처럼
 * 사용자에게 보여줄 수 없는 형태다. 연결 실패만 따로 가르고 나머지는 [fallback] 으로 묶는다.
 */
internal fun Throwable.toUserMessage(fallback: String): String =
    if (this is ConnectionException) NETWORK_ERROR_MESSAGE else fallback

/**
 * 사용자가 조치할 수 있는 서버 안내면 그 문구, 아니면 null 이다.
 *
 * 각 서비스의 오류(`PLACE_*` 지원하지 않는 링크, `USER_*` 카카오 인증 실패 등)는 사용자가 고칠 수 있어 보여 줄 만하다.
 * 서버 오류(5xx)와 공통 오류(`COMMON_*` - 요청 형식·인증·서버 내부 오류)는 사용자가 할 수 있는 게 없고, 본문을
 * 읽지 못한 응답의 문구는 `Bad Gateway` 같은 HTTP 상태 문구(영어)라 쓰지 않는다.
 */
internal fun ApiException.actionableMessage(): String? = serverMessage.takeIf {
    it.isNotBlank() && status < 500 && !code.startsWith("COMMON_") && code != UNKNOWN_ERROR_CODE
}

/**
 * 사진 업로드 실패 안내. 등록·저장 실패와 다르게 안내한다.
 *
 * 형식이나 크기 때문에 걸린 것은 사용자가 사진을 바꿔야 하는 일이라, 예외가 들고 있는 이유를
 * 그대로 보여준다. "사진을 올리지 못했어요" 로 뭉개면 무엇을 고쳐야 하는지 알 수 없다.
 */
internal fun Throwable.toUploadMessage(fallback: String = PHOTO_UPLOAD_FAILED_MESSAGE): String =
    if (this is ImageUploadException) message ?: fallback else toUserMessage(fallback)
