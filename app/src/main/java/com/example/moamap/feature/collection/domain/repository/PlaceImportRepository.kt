package com.example.moamap.feature.collection.domain.repository

import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.PlaceSaveResult

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

    /**
     * 좌표 한 점 근처의 장소를 찾는다. 워치가 보낸 단일 좌표에서 들어온다.
     *
     * @return 0건 아니면 1건
     * @throws com.example.moamap.feature.collection.domain.model.PlaceExtractionException
     *   그 자리에서 등록할 만한 장소를 찾지 못한 경우
     */
    suspend fun findPlaceAtCoordinate(lat: Double, lng: Double): List<ImportedPlace>

    /**
     * 고른 장소를 고른 지도에 모두 등록한다.
     *
     * 서버가 요청 하나에 지도 하나만 받으므로 지도 수만큼 호출한다. 건별로 부분 성공하니
     * 결과는 통과·중복·실패를 합산해 돌려준다.
     *
     * @throws com.example.moamap.core.network.NetworkException 서버 호출이 실패한 경우
     */
    suspend fun savePlaces(mapIds: Set<Long>, places: List<ImportedPlace>): PlaceSaveResult
}
