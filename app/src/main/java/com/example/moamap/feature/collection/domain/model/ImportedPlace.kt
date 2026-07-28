package com.example.moamap.feature.collection.domain.model

/**
 * 인스타그램 URL 에서 뽑아낸 장소 후보.
 *
 * 서버 응답에는 고유 id 가 없어 [id] 는 `kakaoPlaceId` 를 쓰고, 그마저 없으면 순번으로 채운다.
 * 화면에서 어떤 장소를 골랐는지 구분하는 용도다.
 */
data class ImportedPlace(
    val id: String,
    val name: String,
    val address: String,
)
