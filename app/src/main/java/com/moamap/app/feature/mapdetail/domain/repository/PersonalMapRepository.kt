package com.moamap.app.feature.mapdetail.domain.repository

/** 가입할 때 자동으로 생기는 "나만의 지도"를 다룬다. */
interface PersonalMapRepository {

    /**
     * 다른 지도의 장소를 나만의 지도에 그대로 담는다.
     *
     * 이름·주소·위치·분류·설명·태그·사진을 원래 장소에서 옮긴다. 나만의 지도에 이미 같은
     * 장소가 있으면 서버가 `PLACE_010` 으로 막는다. 나만의 지도를 찾지 못하면
     * [PersonalMapNotFoundException] 을 던진다.
     */
    suspend fun addPlace(placeId: Long)
}

/** 내 지도 목록에 나만의 지도가 없다. 가입 직후 서버가 아직 만들지 못한 경우다. */
class PersonalMapNotFoundException : Exception("나만의 지도를 찾지 못했습니다")
