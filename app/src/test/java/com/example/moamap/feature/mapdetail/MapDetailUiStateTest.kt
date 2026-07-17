package com.example.moamap.feature.mapdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapDetailUiStateTest {
    @Test
    fun `default state uses places tab all category and no selected place`() {
        val state = MapDetailUiState()

        assertEquals(MapDetailTab.Places, state.selectedTab)
        assertEquals("전체", state.selectedCategory)
        assertNull(state.selectedPlaceId)
    }

    @Test
    fun `selecting logs changes only the selected tab`() {
        val original = MapDetailUiState(
            selectedCategory = "카페",
            selectedPlaceId = 42L,
        )

        val updated = original.selectTab(MapDetailTab.Logs)

        assertEquals(MapDetailTab.Logs, updated.selectedTab)
        assertEquals(original.selectedCategory, updated.selectedCategory)
        assertEquals(original.selectedPlaceId, updated.selectedPlaceId)
    }

    @Test
    fun `selecting cafe changes the category`() {
        val updated = MapDetailUiState().selectCategory("카페")

        assertEquals("카페", updated.selectedCategory)
    }

    @Test
    fun `selecting a place records its id and closing detail clears it`() {
        val selected = MapDetailUiState().selectPlace(7L)

        assertEquals(7L, selected.selectedPlaceId)
        assertNull(selected.closePlaceDetail().selectedPlaceId)
    }

    @Test
    fun `filter places returns all items for all category and matches otherwise`() {
        val places = listOf(
            place(id = 1L, category = "데이트"),
            place(id = 2L, category = "카페"),
            place(id = 3L, category = "카페"),
        )

        assertEquals(places, filterPlaces(places, "전체"))
        assertEquals(listOf(places[1], places[2]), filterPlaces(places, "카페"))
    }

    private fun place(id: Long, category: String) = PlaceUiModel(
        id = id,
        name = "Place $id",
        description = "Description $id",
        category = category,
        area = "Area $id",
        address = "Address $id",
        rating = 4.5,
        reviewCount = 10,
        favorite = false,
    )
}
