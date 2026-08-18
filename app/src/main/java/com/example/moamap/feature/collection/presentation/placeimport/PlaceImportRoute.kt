package com.example.moamap.feature.collection.presentation.placeimport

import android.net.Uri

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
    const val EDIT = "place_import/edit"
    const val MAP = "place_import/map"

    /** 장소 하나를 편집하는 화면. */
    const val ARG_PLACE_ID = "placeId"
    const val EDIT_DETAIL = "place_import/edit/{$ARG_PLACE_ID}"

    fun editDetail(placeId: String): String = "place_import/edit/${Uri.encode(placeId)}"
}
