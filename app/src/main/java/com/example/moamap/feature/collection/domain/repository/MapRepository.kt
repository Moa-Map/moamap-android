package com.example.moamap.feature.collection.domain.repository

import com.example.moamap.feature.collection.domain.model.CreatedMap
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.collection.domain.model.MyMap
import com.example.moamap.feature.collection.domain.model.NewMap

interface MapRepository {

    /** 내가 참여한 지도 목록. 모음 화면의 탭 하나가 한 번 호출한다. */
    suspend fun getMyMaps(type: MapType): List<MyMap>

    /**
     * 지도를 만든다.
     *
     * 커버 이미지는 포함하지 않는다. 서버가 `imageUrl` 로 URL 문자열을 받는데, 기기 안의
     * 사진을 URL 로 바꿔줄 엔드포인트가 아직 없다. 발급 API 가 생기면 여기에 붙인다.
     */
    suspend fun createMap(newMap: NewMap): CreatedMap

    /** 초대 코드로 프라이빗 지도에 합류하고, 합류한 지도의 id 를 돌려준다. */
    suspend fun joinByInviteCode(inviteCode: String): Long
}
