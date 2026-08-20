package com.moamap.app.feature.collection.domain.repository

import com.moamap.app.feature.collection.domain.model.CreatedMap
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.domain.model.NewMap

interface MapRepository {

    /** 내가 참여한 지도 목록. 모음 화면의 탭 하나가 한 번 호출한다. */
    suspend fun getMyMaps(type: MapType): List<MyMap>

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

    /** 초대 코드로 프라이빗 지도에 합류하고, 합류한 지도의 id 를 돌려준다. */
    suspend fun joinByInviteCode(inviteCode: String): Long
}
