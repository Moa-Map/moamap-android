package com.example.moamap.feature.mapdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapDetailUiStateTest {
    @Test
    fun `default state uses places tab empty query and no selected place`() {
        val state = MapDetailUiState()

        assertEquals(MapDetailTab.Places, state.selectedTab)
        assertEquals("", state.searchQuery)
        assertNull(state.selectedPlaceId)
    }

    @Test
    fun `selecting logs changes only the selected tab`() {
        val original = MapDetailUiState(
            selectedPlaceId = 42L,
            searchQuery = "커피",
        )

        val updated = original.selectTab(MapDetailTab.Logs)

        assertEquals(MapDetailTab.Logs, updated.selectedTab)
        assertEquals(original.searchQuery, updated.searchQuery)
        assertEquals(original.selectedPlaceId, updated.selectedPlaceId)
    }

    @Test
    fun `typing a query records it`() {
        val updated = MapDetailUiState().search("커피")

        assertEquals("커피", updated.searchQuery)
    }

    @Test
    fun `selecting a place records its id and closing detail clears it`() {
        val selected = MapDetailUiState().selectPlace(7L)

        assertEquals(7L, selected.selectedPlaceId)
        assertNull(selected.closePlaceDetail().selectedPlaceId)
    }

    @Test
    fun `빈 검색어는 전부 남긴다`() {
        val places = listOf(place(1L, "커피나무"), place(2L, "달빛정원"))

        assertEquals(places, searchPlaces(places, ""))
        // 공백만 친 것도 안 친 것으로 본다.
        assertEquals(places, searchPlaces(places, "   "))
    }

    @Test
    fun `이름에 검색어가 들어간 장소만 남는다`() {
        val places = listOf(place(1L, "커피나무"), place(2L, "달빛정원"), place(3L, "커피가게"))

        val found = searchPlaces(places, "커피")

        assertEquals(listOf(1L, 3L), found.map { place -> place.id })
    }

    @Test
    fun `검색어 앞뒤 공백은 무시한다`() {
        val places = listOf(place(1L, "커피나무"), place(2L, "달빛정원"))

        assertEquals(listOf(1L), searchPlaces(places, "  커피 ").map { place -> place.id })
    }

    @Test
    fun `대소문자를 가리지 않는다`() {
        val places = listOf(place(1L, "Coffee Tree"), place(2L, "달빛정원"))

        assertEquals(listOf(1L), searchPlaces(places, "coffee").map { place -> place.id })
    }

    @Test
    fun `맞는 게 없으면 빈 목록이다`() {
        val places = listOf(place(1L, "커피나무"))

        assertTrue(searchPlaces(places, "국밥").isEmpty())
    }

    @Test
    fun `주소는 검색 대상이 아니다`() {
        // "서울" 로 거르면 서울 지도가 통째로 남아 거른 티가 안 난다.
        val places = listOf(place(1L, "커피나무", address = "서울 성동구 성수이로 12"))

        assertTrue(searchPlaces(places, "성동구").isEmpty())
    }

    private fun place(
        id: Long,
        name: String,
        address: String = "Address $id",
    ) = PlaceUiModel(
        id = id,
        name = name,
        description = "Description $id",
        category = "카페",
        area = "Area $id",
        address = address,
        rating = 4.5,
        reviewCount = 10,
        favorite = false,
    )
}
