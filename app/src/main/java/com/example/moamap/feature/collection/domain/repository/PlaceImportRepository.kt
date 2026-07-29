package com.example.moamap.feature.collection.domain.repository

import com.example.moamap.feature.collection.domain.model.ImportedPlace

interface PlaceImportRepository {

    /**
     * 인스타그램 URL 의 캡션을 읽어 서버에 넘기고 장소 후보를 받아온다.
     *
     * @throws com.example.moamap.feature.collection.domain.model.PlaceExtractionException 캡션을 읽지 못한 경우
     * @throws com.example.moamap.core.network.NetworkException 서버 호출이 실패한 경우
     */
    suspend fun extractPlaces(url: String): List<ImportedPlace>

    /**
     * 네이버·카카오·구글 지도 공유 링크를 서버에 넘겨 그 리스트의 장소를 받아온다.
     *
     * 인스타그램과 달리 앱이 링크를 열어볼 일이 없다. 서버가 직접 리스트를 읽는다.
     *
     * @throws com.example.moamap.core.network.NetworkException 서버 호출이 실패한 경우
     */
    suspend fun extractMapSharePlaces(url: String): List<ImportedPlace>
}
