package com.example.moamap.feature.collection.data.repository

import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.PlaceExtractionException
import com.example.moamap.feature.collection.domain.repository.PlaceImportRepository
import com.example.moamap.feature.collection.instagram.CaptionExtractor
import com.example.moamap.feature.collection.instagram.CaptionResult
import com.example.moamap.feature.explore.data.remote.InstagramExtractRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceCandidateDto
import com.example.moamap.feature.explore.data.remote.PlaceService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캡션은 앱이 직접 긁고, 장소 해석만 서버에 맡긴다.
 *
 * 서버가 `description`(캡션 전문)을 필수로 요구하므로 캡션 추출이 먼저다.
 * 캡션을 못 읽으면 서버를 호출할 것도 없이 여기서 끝난다.
 */
@Singleton
class PlaceImportRepositoryImpl @Inject constructor(
    private val captionExtractor: CaptionExtractor,
    private val placeService: PlaceService,
) : PlaceImportRepository {

    override suspend fun extractPlaces(url: String): List<ImportedPlace> {
        val caption = when (val result = captionExtractor.extract(url)) {
            is CaptionResult.Success -> result.description
            CaptionResult.Blocked -> throw PlaceExtractionException.CaptionBlocked()
            is CaptionResult.Error -> throw PlaceExtractionException.CaptionUnavailable()
        }

        val candidates = placeService.extractFromInstagram(
            InstagramExtractRequestDto(url = url, description = caption),
        )

        return candidates
            // 이름 없는 후보는 카드에 빈 줄로 보이므로 거른다.
            .filter { candidate -> !candidate.name.isNullOrBlank() }
            .mapIndexed { index, candidate -> candidate.toImportedPlace(index) }
    }
}

private fun PlaceCandidateDto.toImportedPlace(index: Int) = ImportedPlace(
    id = kakaoPlaceId?.takeIf { it.isNotBlank() } ?: "candidate-$index",
    name = name.orEmpty(),
    // 도로명이 사용자에게 익숙하다. 없으면 지번으로 대체한다.
    address = roadAddress?.takeIf { it.isNotBlank() } ?: address.orEmpty(),
)
