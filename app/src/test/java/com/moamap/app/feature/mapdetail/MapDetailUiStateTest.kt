package com.moamap.app.feature.mapdetail

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

    // ---------- 묶음 마커 펼치기 ----------

    @Test
    fun `묶음을 펼치면 그 장소들을 기억하고 닫으면 비운다`() {
        val expanded = MapDetailUiState().expandCluster(listOf(7L, 8L))
        assertEquals(listOf(7L, 8L), expanded.expandedClusterPlaceIds)

        assertTrue(expanded.closeCluster().expandedClusterPlaceIds.isEmpty())
    }

    @Test
    fun `묶음 목록에서 장소를 고르면 목록은 닫힌다`() {
        // 두 시트가 겹쳐 뜨면 상세 뒤에 목록이 남아 뒤로가기가 두 번 필요해진다.
        val state = MapDetailUiState()
            .expandCluster(listOf(7L, 8L))
            .selectPlace(8L)

        assertEquals(8L, state.selectedPlaceId)
        assertTrue(state.expandedClusterPlaceIds.isEmpty())
    }

    @Test
    fun `상세를 닫아도 펼친 묶음은 건드리지 않는다`() {
        // 상세는 묶음 목록 말고 마커에서도 열린다. 닫기가 목록까지 지우면 안 된다.
        val state = MapDetailUiState(expandedClusterPlaceIds = listOf(7L, 8L))
            .closePlaceDetail()

        assertEquals(listOf(7L, 8L), state.expandedClusterPlaceIds)
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
