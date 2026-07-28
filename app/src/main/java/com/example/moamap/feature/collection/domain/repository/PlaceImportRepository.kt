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
}
