package com.moamap.app.feature.collection.data.repository

import android.net.Uri
import com.moamap.app.core.common.upload.PhotoUploader
import com.moamap.app.core.common.upload.validateImageUpload
import com.moamap.app.feature.collection.data.remote.CoverUploadUrlRequestDto
import com.moamap.app.feature.collection.data.remote.JoinByInviteCodeRequestDto
import com.moamap.app.feature.collection.data.remote.MapOrderUpdateRequestDto
import com.moamap.app.feature.collection.data.remote.MapService
import com.moamap.app.feature.collection.domain.model.CreatedMap
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.domain.model.NewMap
import com.moamap.app.feature.collection.domain.repository.MapRepository
import com.moamap.app.feature.mapdetail.data.repository.toMapDetail
import com.moamap.app.feature.mapdetail.domain.model.LeaveOutcome
import com.moamap.app.feature.mapdetail.domain.model.leaveOutcome
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
// PhotoUploader 가 internal 이라 함께 internal 이다. PlaceAddRepositoryImpl 도 같다.
internal class MapRepositoryImpl @Inject constructor(
    private val mapService: MapService,
    private val uploader: PhotoUploader,
) : MapRepository {

    override suspend fun getMyMaps(type: MapType): List<MyMap> = mapService
        .getMyMaps(type = type.requestValue, size = MY_MAPS_SIZE)
        .content
        .map { dto -> dto.toMyMap() }

    override suspend fun updateMyMapOrder(type: MapType, mapIds: List<Long>) =
        mapService.updateMyMapOrder(MapOrderUpdateRequestDto(type = type.requestValue, mapIds = mapIds))

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

    // 지도 상세와 같은 규칙을 쓴다. 제작자 이름은 판단에 필요 없어 조회하지 않는다.
    override suspend fun getLeaveOutcome(mapId: Long): LeaveOutcome? =
        mapService.getMap(mapId).toMapDetail(ownerName = null).leaveOutcome

    override suspend fun leaveMap(mapId: Long) {
        // 고른 뒤에 다른 사람이 들어왔을 수 있다. 되돌릴 수 없는 삭제라 나가는 순간의 상태로
        // 다시 판단한다 - 편집을 시작할 때 본 값을 믿고 지우면 남의 지도까지 사라진다.
        when (getLeaveOutcome(mapId)) {
            LeaveOutcome.DeleteMap -> mapService.deleteMap(mapId)
            LeaveOutcome.Leave, LeaveOutcome.LeaveNeedsInviteCode -> mapService.leaveMap(mapId)
            null -> error("나갈 수 없는 지도다 (mapId=$mapId)")
        }
    }

    private companion object {
        /**
         * 내 지도는 한 번에 다 받는다. 순서를 저장할 때 그 종류의 지도를 빠짐없이 보내야 해서,
         * 앞 페이지만 들고 있으면 서버가 거절한다. 300 은 순서 저장 요청이 받는 최대 개수다.
         */
        // ponytail: 300개를 넘으면 뒤는 안 보이고 순서 저장도 거절된다. 그럴 일이 생기면 페이지를 이어 받는다.
        const val MY_MAPS_SIZE = 300
    }
}
