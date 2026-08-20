package com.moamap.app.feature.collection.domain.model

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

    /**
     * 인스타그램에 닿지 못했다.
     *
     * 링크는 멀쩡한데 통신이 안 되는 경우라, 링크를 확인하라고 안내하면 엉뚱한 조치를 하게 된다.
     */
    class CaptionNetworkError : PlaceExtractionException("네트워크에 연결할 수 없어요")

    /**
     * 좌표는 받았는데 그 근처에서 등록할 만한 장소를 찾지 못했다.
     *
     * 카카오 쿼터가 소진돼도 200 에 빈 결과로 오므로 이 경우와 구분되지 않는다.
     */
    class NoPlaceAtCoordinate : PlaceExtractionException("이 위치 근처에서 장소를 찾지 못했어요")
}
