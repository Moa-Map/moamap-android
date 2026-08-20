package com.moamap.app.feature.collection.data.repository

import android.net.Uri
import com.moamap.app.core.common.upload.PhotoUploader
import com.moamap.app.core.common.upload.validateImageUpload
import com.moamap.app.feature.collection.data.remote.CoverUploadUrlRequestDto
import com.moamap.app.feature.collection.data.remote.JoinByInviteCodeRequestDto
import com.moamap.app.feature.collection.data.remote.MapService
import com.moamap.app.feature.collection.domain.model.CreatedMap
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.domain.model.NewMap
import com.moamap.app.feature.collection.domain.repository.MapRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
// PhotoUploader 가 internal 이라 함께 internal 이다. PlaceAddRepositoryImpl 도 같다.
internal class MapRepositoryImpl @Inject constructor(
    private val mapService: MapService,
    private val uploader: PhotoUploader,
) : MapRepository {

    override suspend fun getMyMaps(type: MapType): List<MyMap> = mapService
        .getMyMaps(type = type.requestValue, size = PAGE_SIZE)
        .content
        .map { dto -> dto.toMyMap() }

    /**
     * 살펴보기 → 검증 → 발급 → 업로드 순으로 간다.
     *
     * 내용은 살펴볼 때 읽지 않는다. 올리는 순간 URI 에서 곧바로 흘려보낸다.
     */
    override suspend fun uploadCoverImage(imageUri: String): String {
        val photo = uploader.inspect(Uri.parse(imageUri))
        validateImageUpload(contentType = photo.contentType, fileSize = photo.size)

        val issued = mapService.createCoverUploadUrl(
            CoverUploadUrlRequestDto(contentType = photo.contentType, fileSize = photo.size),
        )
        require(issued.uploadUrl.isNotBlank() && issued.fileUrl.isNotBlank()) {
            "커버 이미지 업로드 주소가 비어 있습니다"
        }

        uploader.upload(uploadUrl = issued.uploadUrl, photo = photo)
        return issued.fileUrl
    }

    override suspend fun createMap(newMap: NewMap): CreatedMap =
        mapService.createMap(newMap.toCreateRequest()).toCreatedMap()

    override suspend fun joinByInviteCode(inviteCode: String): Long =
        mapService.joinByInviteCode(JoinByInviteCodeRequestDto(inviteCode.trim())).id

    private companion object {
        /** 탐색 탭과 같은 값. 무한 스크롤을 붙이기 전까지는 첫 페이지만 쓴다. */
        const val PAGE_SIZE = 20
    }
}
