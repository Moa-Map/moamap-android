package com.example.moamap.feature.collection.domain.repository

import com.example.moamap.feature.collection.domain.model.EditedPlace
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
     * 편집에서 붙인 사진을 올리고 장소 id 로 찾을 수 있게 돌려준다.
     *
     * 등록과 나눠 둔다. 등록이 실패해 다시 시도할 때 **이미 올린 사진을 재사용**하기 위해서다 -
     * 올린 사진을 지울 API 가 없어, 시도할 때마다 올리면 고아 파일이 그만큼 쌓인다.
     *
     * 여러 지도에 넣더라도 한 번만 부른다. 같은 파일이라 지도 수만큼 올릴 이유가 없다.
     *
     * @param mapId 발급 권한이 장소 등록 권한과 같아 서버가 요구한다. 고른 지도 중 아무거나면 된다.
     * @throws com.example.moamap.core.network.NetworkException 서버 호출이 실패한 경우
     */
    suspend fun uploadPhotos(mapId: Long, places: List<EditedPlace>): Map<String, List<String>>

    /**
     * 고른 장소를 편집값과 함께 고른 지도에 모두 등록한다.
     *
     * 서버가 요청 하나에 지도 하나만 받으므로 지도 수만큼 호출한다. 건별로 부분 성공하니
     * 결과는 통과·중복·실패를 합산해 돌려준다.
     *
     * @param photoUrls [uploadPhotos] 가 돌려준 값. 사진을 붙이지 않았으면 빈 map 이다.
     * @throws com.example.moamap.core.network.NetworkException 서버 호출이 실패한 경우
     */
    suspend fun savePlaces(
        mapIds: Set<Long>,
        places: List<EditedPlace>,
        photoUrls: Map<String, List<String>>,
    ): PlaceSaveResult
}
