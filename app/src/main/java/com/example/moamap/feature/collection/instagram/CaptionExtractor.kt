package com.example.moamap.feature.collection.instagram

/**
 * 캡션 추출 경계.
 *
 * [InstagramCaptionExtractor] 는 실제 네트워크를 타므로, 이 인터페이스를 두고
 * 테스트에서는 가짜 구현으로 바꿔 끼운다.
 */
interface CaptionExtractor {

    suspend fun extract(rawUrl: String): CaptionResult
}
