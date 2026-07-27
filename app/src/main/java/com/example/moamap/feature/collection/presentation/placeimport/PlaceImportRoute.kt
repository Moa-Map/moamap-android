package com.example.moamap.feature.collection.presentation.placeimport

/**
 * 장소 가져오기 중첩 그래프 안의 경로.
 *
 * 그래프 자체의 경로는 `MoaMapRoute.PlaceImport` 다. 이 경로들은 그래프 밖에서 직접
 * 이동할 일이 없어 앱 전역 route 정의와 분리해 둔다.
 */
internal object PlaceImportRoute {
    const val URL = "place_import/url"
    const val LOADING = "place_import/loading"
    const val PLACE = "place_import/place"
    const val MAP = "place_import/map"
}
