package com.moamap.app.feature.mapdetail

import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.domain.model.PlaceCategoryGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapDetailMarkerModelsTest {

    private fun place(category: String) = MapPlace(
        id = 1L,
        name = "커피나무",
        address = "서울 동작구 상도로 369",
        latitude = 37.4963,
        longitude = 126.9574,
        photoUrl = null,
        category = category,
    )

    @Test
    fun `마커는 장소 분류 경로로 카테고리 그룹을 정한다`() {
        assertEquals(
            PlaceCategoryGroup.Cafe,
            place("음식점 > 카페 > 커피전문점").toPlaceMarker().categoryGroup,
        )
    }

    @Test
    fun `분류가 없는 장소 마커는 그룹이 없다`() {
        assertNull(place("").toPlaceMarker().categoryGroup)
    }
}
