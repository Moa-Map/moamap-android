package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.collection.data.remote.MapService
import com.moamap.app.feature.explore.data.remote.PlaceCreateRequestDto
import com.moamap.app.feature.explore.data.remote.PlaceDto
import com.moamap.app.feature.explore.data.remote.PlaceService
import com.moamap.app.feature.mapdetail.domain.repository.PersonalMapNotFoundException
import com.moamap.app.feature.mapdetail.domain.repository.PersonalMapRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 나만의 지도는 프라이빗 목록에 섞여 내려온다. `personal` 로만 가릴 수 있다. */
private const val PRIVATE_MAP_TYPE = "PRIVATE"

private const val MY_MAP_PAGE_SIZE = 50

/** 나만의 지도를 찾으며 넘겨 볼 페이지 수. 보통 첫 페이지에서 끝난다. */
private const val MAX_MY_MAP_PAGES = 20

/** 출처가 비어 있는 옛 장소. 서버 필수값이라 카카오 검색으로 등록된 것으로 본다. */
private const val DEFAULT_SOURCE_TYPE = "KAKAO_SEARCH"

@Singleton
class PersonalMapRepositoryImpl @Inject constructor(
    private val placeService: PlaceService,
    private val mapService: MapService,
) : PersonalMapRepository {

    /**
     * 목록에서 들고 있던 값 대신 장소를 다시 읽는다. 지도 화면은 태그·사진 전체와 카카오 장소
     * id 를 들고 있지 않고, 그 사이 원래 장소가 고쳐졌을 수도 있다.
     *
     * 나만의 지도 id 는 기억해 두지 않는다. 다른 계정으로 다시 로그인하면 달라진다.
     */
    override suspend fun addPlace(placeId: Long) {
        val place = placeService.getPlace(placeId)
        val personalMapId = findPersonalMapId() ?: throw PersonalMapNotFoundException()
        placeService.createPlace(place.toCopyRequest(personalMapId))
    }

    private suspend fun findPersonalMapId(): Long? {
        for (page in 0 until MAX_MY_MAP_PAGES) {
            val response = mapService.getMyMaps(
                type = PRIVATE_MAP_TYPE,
                page = page,
                size = MY_MAP_PAGE_SIZE,
            )
            response.content.firstOrNull { map -> map.personal }?.let { map -> return map.id }
            if (response.last || response.content.isEmpty()) return null
        }
        return null
    }
}

/**
 * 원래 장소를 [mapId] 에 새로 등록하는 요청.
 *
 * 사진은 올린 주소를 그대로 쓴다. 서버가 `photoUrls` 의 출처를 따지지 않는다.
 * 카카오 장소 id 가 없으면 등록할 수 없다(서버 필수값) - 조용히 빈 값으로 보내지 않고 멈춘다.
 */
internal fun PlaceDto.toCopyRequest(mapId: Long): PlaceCreateRequestDto {
    val kakaoId = kakaoPlaceId?.takeIf { id -> id.isNotBlank() }
    checkNotNull(kakaoId) { "카카오 장소 id 가 없는 장소는 옮길 수 없습니다 (placeId=$id)" }

    return PlaceCreateRequestDto(
        name = name.orEmpty(),
        address = address?.takeIf { it.isNotBlank() },
        roadAddress = roadAddress?.takeIf { it.isNotBlank() },
        lat = lat,
        lng = lng,
        category = category?.takeIf { it.isNotBlank() },
        kakaoPlaceId = kakaoId,
        sourceType = sourceType?.takeIf { it.isNotBlank() } ?: DEFAULT_SOURCE_TYPE,
        sourceUrl = sourceUrl?.takeIf { it.isNotBlank() },
        description = description?.takeIf { it.isNotBlank() },
        mapId = mapId,
        tags = tags.takeIf { it.isNotEmpty() },
        photoUrls = photoUrls.filter { url -> url.isNotBlank() }.takeIf { it.isNotEmpty() },
    )
}
