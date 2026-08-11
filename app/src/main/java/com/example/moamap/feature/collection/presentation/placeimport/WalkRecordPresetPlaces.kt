package com.example.moamap.feature.collection.presentation.placeimport

import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.PlaceImportSource

/**
 * 임시. 워치 기록에서 추천 흐름을 열었을 때 서버 대신 쓰는 하드코딩 목록.
 *
 * 좌표와 주소는 숭실대 주변을 어림잡은 값이라 정확하지 않다. 화면 흐름만 확인하는 용도다.
 *
 * `kakaoPlaceId` 는 카드를 고를 수 있게 하려고 채워둔 가짜 값이다. 실제로 지도에 저장하면
 * 서버가 없는 키라며 거절한다. 추천 API 가 붙으면 이 파일과 [PlaceImportSource.WalkRecordSingle]
 * · [PlaceImportSource.WalkRecordMulti] 를 함께 지운다.
 */
internal fun walkRecordPresetPlaces(source: PlaceImportSource): List<ImportedPlace> =
    when (source) {
        PlaceImportSource.WalkRecordSingle -> SoongsilOnly
        else -> SoongsilNearby
    }

private val Soongsil = ImportedPlace(
    id = "walk-preset-1",
    name = "숭실대학교",
    address = "서울 동작구 상도동 511",
    roadAddress = "서울 동작구 상도로 369",
    lat = 37.4963,
    lng = 126.9574,
    category = "학교",
    kakaoPlaceId = "walk-preset-1",
    sourceType = "WALK_RECORD",
)

private val SoongsilOnly = listOf(Soongsil)

private val SoongsilNearby = listOf(
    Soongsil,
    ImportedPlace(
        id = "walk-preset-2",
        name = "숭실대입구역",
        address = "서울 동작구 상도동 355",
        roadAddress = "서울 동작구 상도로 지하 272",
        lat = 37.4962,
        lng = 126.9536,
        category = "지하철역",
        kakaoPlaceId = "walk-preset-2",
        sourceType = "WALK_RECORD",
    ),
    ImportedPlace(
        id = "walk-preset-3",
        name = "상도근린공원",
        address = "서울 동작구 상도동 234",
        roadAddress = "서울 동작구 상도로 47길 6",
        lat = 37.4998,
        lng = 126.9490,
        category = "공원",
        kakaoPlaceId = "walk-preset-3",
        sourceType = "WALK_RECORD",
    ),
    ImportedPlace(
        id = "walk-preset-4",
        name = "스타벅스 숭실대점",
        address = "서울 동작구 상도동 361",
        roadAddress = "서울 동작구 상도로 356",
        lat = 37.4959,
        lng = 126.9558,
        category = "카페",
        kakaoPlaceId = "walk-preset-4",
        sourceType = "WALK_RECORD",
    ),
    ImportedPlace(
        id = "walk-preset-5",
        name = "국사봉근린공원",
        address = "서울 동작구 상도동 산65",
        roadAddress = "서울 동작구 양녕로 92",
        lat = 37.4915,
        lng = 126.9430,
        category = "공원",
        kakaoPlaceId = "walk-preset-5",
        sourceType = "WALK_RECORD",
    ),
)
