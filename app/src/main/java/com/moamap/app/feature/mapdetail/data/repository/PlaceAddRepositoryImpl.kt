package com.moamap.app.feature.mapdetail.data.repository

import android.net.Uri
import com.moamap.app.core.common.upload.PhotoUploader
import com.moamap.app.feature.explore.data.remote.PlaceCreateRequestDto
import com.moamap.app.feature.explore.data.remote.PlaceService
import com.moamap.app.feature.explore.data.remote.uploadPlacePhotos
import com.moamap.app.feature.mapdetail.domain.model.NewPlace
import com.moamap.app.feature.mapdetail.domain.repository.PlaceAddRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 이 화면은 카카오 검색으로 찾은 장소만 등록한다. */
private const val SOURCE_TYPE_KAKAO_SEARCH = "KAKAO_SEARCH"

@Singleton
internal class PlaceAddRepositoryImpl @Inject constructor(
    private val placeService: PlaceService,
    private val uploader: PhotoUploader,
) : PlaceAddRepository {

    override suspend fun uploadPhotos(mapId: Long, photos: List<Uri>): List<String> =
        placeService.uploadPlacePhotos(uploader = uploader, mapId = mapId, photos = photos)

    override suspend fun addPlace(mapId: Long, newPlace: NewPlace) {
        val candidate = newPlace.candidate

        placeService.createPlace(
            PlaceCreateRequestDto(
                name = candidate.name,
                address = candidate.address,
                roadAddress = candidate.roadAddress,
                lat = candidate.latitude,
                lng = candidate.longitude,
                category = candidate.category,
                kakaoPlaceId = candidate.kakaoPlaceId,
                sourceType = SOURCE_TYPE_KAKAO_SEARCH,
                sourceUrl = candidate.placeUrl,
                description = newPlace.memo?.takeIf { it.isNotBlank() },
                mapId = mapId,
                tags = newPlace.tags.takeIf { it.isNotEmpty() },
                photoUrls = newPlace.photoUrls.takeIf { it.isNotEmpty() },
            ),
        )
    }
}
