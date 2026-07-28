package com.example.moamap.feature.collection.domain.repository

import com.example.moamap.feature.collection.domain.model.NewMap

interface MapRepository {

    /**
     * 지도를 만들고 만들어진 지도의 id 를 돌려준다.
     *
     * 커버 이미지는 포함하지 않는다. 서버가 `imageUrl` 로 URL 문자열을 받는데, 기기 안의
     * 사진을 URL 로 바꿔줄 엔드포인트가 아직 없다. 발급 API 가 생기면 여기에 붙인다.
     */
    suspend fun createMap(newMap: NewMap): Long
}
