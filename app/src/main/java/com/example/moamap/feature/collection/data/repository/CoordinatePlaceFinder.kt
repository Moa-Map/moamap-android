package com.example.moamap.feature.collection.data.repository

import com.example.moamap.core.network.kakao.KakaoLocalService
import com.example.moamap.core.network.kakao.KakaoPlaceDto
import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.PlaceExtractionException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 좌표 하나에서 장소 한 곳을 찾는다.
 *
 * 카카오는 "좌표만 주면 근처 장소" 를 주지 않는다 - 키워드나 카테고리가 필수다. 그래서
 * 좌표를 주소로 바꾼 뒤 그 주소를 키워드로 검색한다. 카테고리 검색(음식점·카페…)을 쓰지
 * 않는 이유는 우리가 카테고리를 고르는 순간 근처에 그 업종이 없으면 0건이 되기 때문이다.
 */
@Singleton
class CoordinatePlaceFinder @Inject constructor(
    private val service: KakaoLocalService,
) {

    /**
     * @return 항상 0건 아니면 1건. 화면이 목록을 기대하므로 리스트로 준다.
     * @throws PlaceExtractionException.NoPlaceAtCoordinate 주소나 장소를 얻지 못한 경우
     */
    suspend fun find(lat: Double, lng: Double): List<ImportedPlace> {
        val x = lng.toString()
        val y = lat.toString()

        val document = service.coordToAddress(lng = x, lat = y).documents.firstOrNull()
        // 도로명이 익숙하니 먼저 쓰고, 없는 곳(산·논밭)에서는 지번으로 내려간다.
        val address = document?.roadAddress?.addressName?.takeIf { it.isNotBlank() }
            ?: document?.address?.addressName?.takeIf { it.isNotBlank() }
            ?: throw PlaceExtractionException.NoPlaceAtCoordinate()

        val place = service.searchKeyword(
            query = address,
            size = 1,
            x = x,
            y = y,
            radius = SEARCH_RADIUS_METERS,
            sort = SORT_BY_DISTANCE,
        ).documents.firstNotNullOfOrNull { dto -> dto.toImportedPlace() }
            ?: throw PlaceExtractionException.NoPlaceAtCoordinate()

        return listOf(place)
    }

    private companion object {
        /** 주소가 가리키는 건물을 담을 만큼만. 넓히면 옆 동네 장소가 딸려 온다. */
        const val SEARCH_RADIUS_METERS = 1_000

        const val SORT_BY_DISTANCE = "distance"
    }
}

/**
 * 카카오 검색 결과를 가져오기 후보로 옮긴다.
 *
 * **`x` 가 경도이고 `y` 가 위도다.** 둘 다 문자열로 와서 바꿔 넣어도 컴파일이 되고,
 * 서울 좌표를 뒤집으면 마커가 중국 근처에 선다.
 *
 * 등록 키([KakaoPlaceDto.id])나 좌표가 없으면 서버가 등록을 거절하므로 후보로 삼지 않는다.
 */
private fun KakaoPlaceDto.toImportedPlace(): ImportedPlace? {
    if (id.isBlank() || placeName.isBlank()) return null

    // toDoubleOrNull 은 "NaN" 과 "Infinity" 도 읽어 낸다. 범위까지 봐야 걸러진다.
    val longitude = x.toDoubleOrNull()?.takeIf { it in -180.0..180.0 } ?: return null
    val latitude = y.toDoubleOrNull()?.takeIf { it in -90.0..90.0 } ?: return null

    return ImportedPlace(
        id = id,
        name = placeName,
        address = addressName?.takeIf { it.isNotBlank() },
        roadAddress = roadAddressName?.takeIf { it.isNotBlank() },
        lat = latitude,
        lng = longitude,
        category = categoryName?.takeIf { it.isNotBlank() },
        kakaoPlaceId = id,
        // 서버가 아는 값만 쓴다. 실제로 카카오 검색에서 나온 장소가 맞다.
        sourceType = KAKAO_SEARCH_SOURCE_TYPE,
        sourceUrl = placeUrl?.takeIf { it.isNotBlank() },
    )
}

private const val KAKAO_SEARCH_SOURCE_TYPE = "KAKAO_SEARCH"
