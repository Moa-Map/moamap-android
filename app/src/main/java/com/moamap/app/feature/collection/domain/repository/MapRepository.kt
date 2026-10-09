package com.moamap.app.feature.collection.domain.repository

import com.moamap.app.feature.collection.domain.model.CreatedMap
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.domain.model.NewMap
import com.moamap.app.feature.mapdetail.domain.model.LeaveOutcome

interface MapRepository {

    /** 내가 참여한 지도 목록. 모음 화면의 탭 하나가 한 번 호출한다. */
    suspend fun getMyMaps(type: MapType): List<MyMap>

    /**
     * 모음 탭의 지도 순서를 저장한다.
     *
     * @param mapIds 그 종류에서 참여 중인 지도 전부를 원하는 순서대로. 빠지거나 겹치면 서버가 거절한다.
     */
    suspend fun updateMyMapOrder(type: MapType, mapIds: List<Long>)

    /**
     * 커버 이미지를 올리고 저장된 주소를 돌려준다.
     *
     * 지도를 만들기 전에 부를 수 있다 - 발급 API 가 `mapId` 를 받지 않는다.
     *
     * @param imageUri 사용자가 고른 사진의 `content://` URI 문자열. 화면 상태가 들고 있는
     *  형태 그대로 받는다.
     * @return 지도 생성 요청의 `imageUrl` 에 담을 주소
     * @throws com.moamap.app.core.common.upload.ImageUploadException
     *  형식이나 크기가 서버 허용 범위를 벗어날 때
     */
    suspend fun uploadCoverImage(imageUri: String): String

    /** 지도를 만든다. 커버는 [uploadCoverImage] 로 먼저 올려 [NewMap.imageUrl] 에 담아 넘긴다. */
    suspend fun createMap(newMap: NewMap): CreatedMap

    /**
     * 지도 이름·설명·사진·태그를 고친다.
     *
     * 서버가 통째로 덮어써서 빠진 값은 지워진다. 그대로 둘 값도 늘 함께 넘긴다.
     *
     * @param imageUrl 올라가 있는 사진 주소. 새 사진은 [uploadCoverImage] 로 먼저 올려 받은 주소를 넣는다.
     */
    suspend fun updateMap(
        mapId: Long,
        name: String,
        description: String?,
        imageUrl: String?,
        tags: List<String>,
    )

    /** 초대 코드로 프라이빗 지도에 합류하고, 합류한 지도의 id 를 돌려준다. */
    suspend fun joinByInviteCode(inviteCode: String): Long

    /**
     * 이 지도에서 나가면 어떻게 되는지. 나갈 수 없으면(방장 등) null 이다.
     *
     * 내 지도 목록 응답에는 내 역할이 없어서 지도 상세를 한 번 더 읽어 판단한다.
     */
    suspend fun getLeaveOutcome(mapId: Long): LeaveOutcome?

    /**
     * 지도에서 나간다. 혼자 남은 프라이빗 지도의 방장이면 지도를 삭제한다.
     *
     * 나갈 수 없는 지도면 예외를 던진다.
     */
    suspend fun leaveMap(mapId: Long)
}
