package com.moamap.app.feature.mapdetail.domain.repository

import android.net.Uri
import com.moamap.app.feature.mapdetail.domain.model.NewPlace

interface PlaceAddRepository {

    /**
     * 사진을 올리고 접근 URL 을 돌려준다.
     *
     * 한 장이라도 실패하면 예외를 던진다. 사진이 빠진 채로 장소가 등록되면 사용자가
     * 알아챌 방법이 없다.
     *
     * @param mapId 발급 권한이 장소 등록 권한과 같아 서버가 요구한다.
     */
    suspend fun uploadPhotos(mapId: Long, photos: List<Uri>): List<String>

    /**
     * 지도에 장소를 등록한다.
     *
     * 승인 대기(`PENDING`)가 될지 바로 등록(`APPROVED`)될지는 **서버가 정한다.**
     * 커뮤니티 지도의 일반 멤버면 승인 대기다.
     */
    suspend fun addPlace(mapId: Long, newPlace: NewPlace)
}
