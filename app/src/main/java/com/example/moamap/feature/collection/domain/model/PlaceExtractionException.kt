package com.example.moamap.feature.collection.domain.model

/**
 * 캡션을 읽지 못해 서버에 보낼 것이 없다.
 *
 * 서버 호출 실패와 구분해야 사용자에게 다른 안내를 줄 수 있다.
 */
sealed class PlaceExtractionException(message: String) : Exception(message) {

    /** 비공개 계정이거나 로그인이 필요한 게시물이라 캡션을 가져올 수 없다. */
    class CaptionBlocked : PlaceExtractionException("비공개 게시물이라 장소를 가져올 수 없어요")

    /** URL 형식이 잘못됐거나 게시물을 읽지 못했다. */
    class CaptionUnavailable : PlaceExtractionException("링크를 다시 확인해주세요")
}
