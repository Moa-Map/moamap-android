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

    /**
     * 임시. 워치 기록에서 들어오는 추천 확인용으로, 장소 한 곳만 준다.
     *
     * 추천 API 가 붙기 전까지 화면 흐름만 확인하려고 둔 값이다. URL 입력도 서버 호출도
     * 없이 하드코딩한 목록을 바로 띄운다. API 가 붙으면 [WalkRecordMulti] 와 함께 지운다.
     */
    WalkRecordSingle,

    /** 임시. [WalkRecordSingle] 과 같고 미리 지정한 장소 여러 곳을 준다. */
    WalkRecordMulti,
    ;

    /** 워치 기록에서 들어온 임시 흐름인지. URL 입력 단계를 건너뛰는 기준이다. */
    val isWalkRecord: Boolean
        get() = this == WalkRecordSingle || this == WalkRecordMulti
}
