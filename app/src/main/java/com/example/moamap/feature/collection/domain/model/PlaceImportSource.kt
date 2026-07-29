package com.example.moamap.feature.collection.domain.model

/**
 * 장소를 어디서 가져오는지. 모음 탭의 두 카드가 각각 하나씩 연다.
 *
 * 흐름과 화면은 같고 부르는 API 와 안내 문구만 갈린다.
 */
enum class PlaceImportSource {

    /** 릴스 링크. 앱이 캡션을 긁어 서버에 넘긴다. */
    Instagram,

    /** 네이버·카카오·구글 지도의 리스트 공유 링크. 서버가 직접 읽는다. */
    MapShare,
}
