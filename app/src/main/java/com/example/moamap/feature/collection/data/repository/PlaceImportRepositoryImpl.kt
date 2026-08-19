package com.example.moamap.feature.collection.data.repository

import com.example.moamap.core.common.upload.PhotoUploader
import com.example.moamap.feature.collection.domain.model.EditedPlace
import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.PlaceExtractionException
import com.example.moamap.feature.collection.domain.model.PlaceSaveResult
import com.example.moamap.feature.collection.domain.repository.PlaceImportRepository
import com.example.moamap.feature.collection.instagram.CaptionExtractor
import com.example.moamap.feature.collection.instagram.CaptionResult
import com.example.moamap.feature.explore.data.remote.InstagramExtractRequestDto
import com.example.moamap.feature.explore.data.remote.MAX_BULK_PLACES
import com.example.moamap.feature.explore.data.remote.MapShareExtractRequestDto
import com.example.moamap.feature.explore.data.remote.MapSharePlaceCandidateDto
import com.example.moamap.feature.explore.data.remote.PlaceBulkCreateRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceBulkItemDto
import com.example.moamap.feature.explore.data.remote.PlaceCandidateDto
import com.example.moamap.feature.explore.data.remote.PlaceService
import com.example.moamap.feature.explore.data.remote.uploadPlacePhotos
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캡션은 앱이 직접 긁고, 장소 해석만 서버에 맡긴다.
 *
 * 서버가 `description`(캡션 전문)을 필수로 요구하므로 캡션 추출이 먼저다.
 * 캡션을 못 읽으면 서버를 호출할 것도 없이 여기서 끝난다.
 */
@Singleton
internal class PlaceImportRepositoryImpl @Inject constructor(
    private val captionExtractor: CaptionExtractor,
    private val placeService: PlaceService,
    private val uploader: PhotoUploader,
) : PlaceImportRepository {

    override suspend fun extractPlaces(url: String): List<ImportedPlace> {
        // 캡션을 읽을 때와 서버에 보낼 때가 같은 URL 이어야 한다.
        // 다른 값을 쓰면 앱은 캡션을 읽었는데 서버는 링크를 거부하는 상황이 생긴다.
        val normalizedUrl = url.trim()

        val caption = when (val result = captionExtractor.extract(normalizedUrl)) {
            is CaptionResult.Success -> result.description
            CaptionResult.Blocked -> throw PlaceExtractionException.CaptionBlocked()
            is CaptionResult.NetworkError -> throw PlaceExtractionException.CaptionNetworkError()
            is CaptionResult.Error -> throw PlaceExtractionException.CaptionUnavailable()
        }

        val candidates = placeService.extractFromInstagram(
            InstagramExtractRequestDto(url = normalizedUrl, description = caption),
        )

        return candidates
            // 이름 없는 후보는 카드에 빈 줄로 보이므로 거른다.
            .filter { candidate -> !candidate.name.isNullOrBlank() }
            .mapIndexed { index, candidate -> candidate.toImportedPlace(index) }
    }

    override suspend fun extractMapSharePlaces(url: String): List<ImportedPlace> {
        val response = placeService.extractFromMapShare(
            MapShareExtractRequestDto(url = url.trim()),
        )

        // 재매칭에 실패한 unmatched 는 등록까지 갈 수 없어 목록에 올리지 않는다.
        return response.matched
            .filter { candidate -> !candidate.name.isNullOrBlank() }
            .mapIndexed { index, candidate ->
                // 후보마다 붙는 값이지만 리스트 전체가 한 지도에서 나온 것이라
                // 비어 있으면 응답 최상단의 출처로 채운다.
                candidate.toImportedPlace(index, candidate.sourceType ?: response.source)
            }
    }

    /**
     * 서버가 요청 하나에 지도 하나만 받아서, 고른 지도마다 따로 부른다.
     *
     * 하나라도 실패하면 그대로 던진다. 일부 지도만 저장된 채 성공했다고 알리면
     * 사용자가 나머지 지도를 다시 시도할 방법이 없다.
     */
    override suspend fun savePlaces(
        mapIds: Set<Long>,
        places: List<EditedPlace>,
        photoUrls: Map<String, List<String>>,
    ): PlaceSaveResult {
        var created = 0
        var duplicate = 0
        var failed = 0

        for (mapId in mapIds) {
            for (chunk in places.chunked(MAX_BULK_PLACES)) {
                val response = placeService.createPlacesBulk(
                    PlaceBulkCreateRequestDto(
                        mapId = mapId,
                        places = chunk.map { entry ->
                            entry.toBulkItem(photoUrls[entry.place.id].orEmpty())
                        },
                    ),
                )
                for (result in response.results) {
                    when (result.status) {
                        CREATED -> created++
                        DUPLICATE -> duplicate++
                        else -> failed++
                    }
                }
            }
        }

        return PlaceSaveResult(created = created, duplicate = duplicate, failed = failed)
    }

    /** 발급이 한 번에 5장까지라 장소별로 나눠 부른다. 사진을 붙이지 않은 장소는 부르지 않는다. */
    override suspend fun uploadPhotos(
        mapId: Long,
        places: List<EditedPlace>,
    ): Map<String, List<String>> = places
        .filter { entry -> entry.edit.photos.isNotEmpty() }
        .associate { entry ->
            entry.place.id to placeService.uploadPlacePhotos(
                uploader = uploader,
                mapId = mapId,
                photos = entry.edit.photos,
            )
        }

    private companion object {
        const val CREATED = "CREATED"
        const val DUPLICATE = "DUPLICATE"
    }
}

/**
 * 편집값을 얹어 일괄 등록 항목으로 만든다.
 *
 * 비어 있는 값은 `null` 로 보낸다. 빈 목록이나 빈 문자열을 보내면 서버가 "지우라는 뜻"으로
 * 받을 수 있고, 어차피 실을 내용이 없다.
 */
private fun EditedPlace.toBulkItem(photoUrls: List<String>) = PlaceBulkItemDto(
    name = place.name,
    address = place.address,
    roadAddress = place.roadAddress,
    lat = place.lat,
    lng = place.lng,
    category = place.category,
    // 고를 수 있었던 장소는 이 값을 갖고 있다. 없는 후보는 선택 단계에서 걸러진다.
    kakaoPlaceId = place.kakaoPlaceId.orEmpty(),
    sourceType = place.sourceType,
    sourceUrl = place.sourceUrl,
    // 편집 화면의 메모가 곧 설명이다. 외부 지도에서 온 기존 메모도 이 값으로 흘러들어온다.
    description = edit.memo.takeIf(String::isNotBlank),
    tags = edit.tags.takeIf(List<String>::isNotEmpty),
    photoUrls = photoUrls.takeIf(List<String>::isNotEmpty),
)

private fun PlaceCandidateDto.toImportedPlace(index: Int) = ImportedPlace(
    id = kakaoPlaceId?.takeIf { it.isNotBlank() } ?: "candidate-$index",
    name = name.orEmpty(),
    address = address,
    roadAddress = roadAddress,
    lat = lat,
    lng = lng,
    category = category,
    kakaoPlaceId = kakaoPlaceId,
    sourceType = INSTAGRAM_SOURCE_TYPE,
    sourceUrl = sourceUrl,
)

private fun MapSharePlaceCandidateDto.toImportedPlace(index: Int, sourceType: String?) =
    ImportedPlace(
        id = kakaoPlaceId?.takeIf { it.isNotBlank() } ?: "candidate-$index",
        name = name.orEmpty(),
        address = address,
        roadAddress = roadAddress,
        lat = lat,
        lng = lng,
        category = category,
        description = description,
        kakaoPlaceId = kakaoPlaceId,
        sourceType = sourceType.orEmpty(),
        sourceUrl = sourceUrl,
    )

/** 인스타그램 추출 응답에는 출처가 없다. 이 경로로 들어온 후보는 전부 릴스에서 나온 것이다. */
private const val INSTAGRAM_SOURCE_TYPE = "INSTAGRAM"
