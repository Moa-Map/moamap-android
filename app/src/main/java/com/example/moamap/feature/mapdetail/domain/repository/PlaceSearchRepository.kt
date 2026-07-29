package com.example.moamap.feature.mapdetail.domain.repository

import com.example.moamap.feature.mapdetail.domain.model.PlaceCandidate

/**
 * 추가할 장소를 찾는다.
 *
 * 지금 구현은 카카오 로컬 API 를 앱에서 직접 부른다. 우리 서버에 장소 검색 엔드포인트가
 * 없어서다. 서버가 검색을 대신하게 되면 **이 인터페이스는 그대로 두고 구현체와 DI 바인딩만
 * 바꾼다** - 그래서 화면과 ViewModel 은 카카오 DTO 를 알면 안 된다.
 */
interface PlaceSearchRepository {

    /** 등록할 수 없는 결과(좌표나 id 가 없는 항목)는 걸러서 돌려준다. */
    suspend fun search(query: String): List<PlaceCandidate>
}
