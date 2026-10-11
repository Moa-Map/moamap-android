package com.moamap.app.core.common

import com.moamap.app.core.common.upload.ImageUploadException
import com.moamap.app.core.network.ConnectionException

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
 * 사진 업로드 실패 안내. 등록·저장 실패와 다르게 안내한다.
 *
 * 형식이나 크기 때문에 걸린 것은 사용자가 사진을 바꿔야 하는 일이라, 예외가 들고 있는 이유를
 * 그대로 보여준다. "사진을 올리지 못했어요" 로 뭉개면 무엇을 고쳐야 하는지 알 수 없다.
 */
internal fun Throwable.toUploadMessage(fallback: String = PHOTO_UPLOAD_FAILED_MESSAGE): String =
    if (this is ImageUploadException) message ?: fallback else toUserMessage(fallback)
