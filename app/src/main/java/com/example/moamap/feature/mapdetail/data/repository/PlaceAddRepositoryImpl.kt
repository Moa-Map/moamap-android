package com.example.moamap.feature.mapdetail.data.repository

import android.net.Uri
import com.example.moamap.feature.explore.data.remote.PhotoFileSpecDto
import com.example.moamap.feature.explore.data.remote.PhotoUploadUrlRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceCreateRequestDto
import com.example.moamap.feature.explore.data.remote.PlaceService
import com.example.moamap.feature.mapdetail.domain.model.NewPlace
import com.example.moamap.feature.mapdetail.domain.repository.PlaceAddRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 이 화면은 카카오 검색으로 찾은 장소만 등록한다. */
private const val SOURCE_TYPE_KAKAO_SEARCH = "KAKAO_SEARCH"

@Singleton
internal class PlaceAddRepositoryImpl @Inject constructor(
    private val placeService: PlaceService,
    private val uploader: PhotoUploader,
) : PlaceAddRepository {

    /**
     * 읽기 → 일괄 발급 → 장별 업로드 순으로 간다.
     *
     * 발급 요청이 각 파일의 크기와 형식을 요구해서 먼저 읽어야 한다. 발급은 한 번에 받고
     * 업로드만 장별로 한다.
     */
    override suspend fun uploadPhotos(mapId: Long, photos: List<Uri>): List<String> {
        if (photos.isEmpty()) return emptyList()

        val read = photos.map { uri -> uploader.read(uri) }

        val issued = placeService.createPhotoUploadUrls(
            PhotoUploadUrlRequestDto(
                mapId = mapId,
                files = read.map { photo ->
                    PhotoFileSpecDto(contentType = photo.contentType, fileSize = photo.size)
                },
            ),
        )

        // 발급 수가 요청 수와 다르면 어떤 사진이 빠졌는지 알 수 없다. 조용히 덜 올리지 않는다.
        check(issued.size == read.size) {
            "사진 업로드 주소를 ${read.size}개 요청했는데 ${issued.size}개 받았습니다"
        }

        issued.forEachIndexed { index, url ->
            uploader.upload(uploadUrl = url.uploadUrl, photo = read[index])
        }

        return issued.map { url -> url.fileUrl }
    }

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
