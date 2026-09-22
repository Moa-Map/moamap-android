package com.moamap.app.feature.mapdetail

import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.domain.model.PlaceCategoryGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceCategoryFilterTest {

    private fun place(id: Long, name: String, category: String) = MapPlace(
        id = id,
        name = name,
        address = "서울 동작구 상도로 369",
        latitude = 37.49,
        longitude = 126.95,
        photoUrl = null,
        category = category,
    ).toPlaceUiModel()

    private val cafe = place(1L, "커피나무", "음식점 > 카페 > 커피전문점")
    private val restaurant = place(2L, "달빛정원", "음식점 > 한식 > 국밥")
    private val pharmacy = place(3L, "온누리약국", "의료,건강 > 약국")
    private val unknown = place(4L, "모아문구", "쇼핑,유통 > 문구")

    @Test
    fun `장소의 분류 경로로 칩 그룹을 정한다`() {
        assertEquals(PlaceCategoryGroup.Cafe, cafe.categoryGroup)
        assertEquals(null, unknown.categoryGroup)
    }

    @Test
    fun `칩은 전체와 이 지도에 있는 카테고리만 카카오 코드 순으로 둔다`() {
        val filters = categoryFilters(listOf(pharmacy, cafe, restaurant))

        assertEquals(
            listOf("전체", "음식점", "카페", "약국"),
            filters.map { filter -> filter.label },
        )
    }

    @Test
    fun `분류를 모르는 장소가 있으면 기타 칩을 맨 뒤에 붙인다`() {
        val filters = categoryFilters(listOf(unknown, cafe))

        assertEquals(listOf("전체", "카페", "기타"), filters.map { filter -> filter.label })
    }

    @Test
    fun `장소가 없으면 전체 칩만 남는다`() {
        assertEquals(listOf(PlaceCategoryFilter.All), categoryFilters(emptyList()))
    }

    @Test
    fun `카테고리와 검색어를 함께 적용한다`() {
        val places = listOf(cafe, restaurant, pharmacy, unknown)

        assertEquals(places, filterPlaces(places, PlaceCategoryFilter.All, ""))
        assertEquals(
            listOf(cafe),
            filterPlaces(places, PlaceCategoryFilter.Group(PlaceCategoryGroup.Cafe), ""),
        )
        assertEquals(listOf(unknown), filterPlaces(places, PlaceCategoryFilter.Other, ""))
        assertEquals(
            listOf(restaurant),
            filterPlaces(places, PlaceCategoryFilter.Group(PlaceCategoryGroup.Restaurant), "달빛"),
        )
        assertTrue(
            filterPlaces(places, PlaceCategoryFilter.Group(PlaceCategoryGroup.Cafe), "달빛").isEmpty(),
        )
    }

    @Test
    fun `고른 칩은 한 칸에 담았다가 그대로 되살린다`() {
        val filters = listOf(
            PlaceCategoryFilter.All,
            PlaceCategoryFilter.Other,
            PlaceCategoryFilter.Group(PlaceCategoryGroup.Pharmacy),
        )

        filters.forEach { filter ->
            assertEquals(filter, placeCategoryFilterOf(filter.saveKey()))
        }
    }

    @Test
    fun `모르는 값으로 되살리면 전체로 돌아간다`() {
        assertEquals(PlaceCategoryFilter.All, placeCategoryFilterOf(null))
        assertEquals(PlaceCategoryFilter.All, placeCategoryFilterOf("Bakery"))
    }
}
