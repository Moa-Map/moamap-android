package com.example.moamap.feature.collection.domain.model

/**
 * 고른 사진을 지도 커버로 쓸 수 없다.
 *
 * 서버 400 을 받고 나서 알리면 "지도를 만들지 못했어요" 밖에 보여줄 수 없다. 무엇이 문제인지
 * 알려주려면 보내기 전에 걸러야 한다.
 */
sealed class CoverImageException(message: String) : Exception(message) {

    /** 서버가 발급해 주지 않는 형식이다. */
    class UnsupportedType : CoverImageException("JPG, PNG, WEBP 형식만 올릴 수 있어요")

    /** 서버 발급 한도를 넘는다. */
    class TooLarge : CoverImageException("사진 크기는 10MB 이하여야 해요")
}
