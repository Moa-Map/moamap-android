package com.moamap.app.feature.mapdetail.domain.repository

import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.domain.model.PlaceLike
import com.moamap.app.feature.mapdetail.domain.model.ReportReason

interface MapDetailRepository {

    /** 지도 한 건. 제작자 닉네임까지 채워서 돌려준다. */
    suspend fun getMapDetail(mapId: Long): MapDetail

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

    /** 하트를 누르거나([liked] = true) 취소한다. 서버가 확정한 상태를 돌려준다. */
    suspend fun setPlaceLiked(placeId: Long, liked: Boolean): PlaceLike

    /**
     * 장소의 메모·태그·사진을 바꾼다. 비운 값도 그대로 보내 서버에서 지운다. 바뀐 장소를 돌려준다.
     *
     * @param photoUrls 남길 사진까지 담은 전체 목록. null 이면 사진은 그대로 둔다.
     */
    suspend fun updatePlace(placeId: Long, memo: String, tags: List<String>, photoUrls: List<String>?): MapPlace

    suspend fun deletePlace(placeId: Long)

    /** 남의 장소를 신고한다. [detail] 은 앞뒤 공백을 지우고 비면 싣지 않는다. */
    suspend fun reportPlace(placeId: Long, reason: ReportReason, detail: String)
}
