package com.example.moamap.feature.mapdetail.domain.repository

import com.example.moamap.feature.mapdetail.domain.model.MapDetail
import com.example.moamap.feature.mapdetail.domain.model.MapPlace
import com.example.moamap.feature.mapdetail.domain.model.MapPlacePreview

interface MapDetailRepository {

    /** 지도 한 건. 제작자 닉네임까지 채워서 돌려준다. */
    suspend fun getMapDetail(mapId: Long): MapDetail

    /**
     * 지도 설명 화면에 얹을 장소 목록.
     *
     * @param visibleCount 화면에 보여줄 개수. 더 있는지 알아내려고 한 건 더 받아 온다.
     */
    suspend fun getPlacePreview(mapId: Long, visibleCount: Int): MapPlacePreview

    /**
     * 지도에 등록된 장소 전부.
     *
     * 상세 화면 마커가 쓴다. 화면에 보이는 것만 골라 받을 수단이 없어 전량을 받는다.
     */
    suspend fun getPlaces(mapId: Long): List<MapPlace>

    /** 공개 지도에 참여한다. 프라이빗 지도는 초대 코드로만 합류하므로 여기로 오지 않는다. */
    suspend fun joinMap(mapId: Long)

    /** 지도에서 나간다. 서버가 OWNER 의 탈퇴는 거절한다. */
    suspend fun leaveMap(mapId: Long)

    /** 지도를 없앤다. OWNER 만 할 수 있다. */
    suspend fun deleteMap(mapId: Long)
}
