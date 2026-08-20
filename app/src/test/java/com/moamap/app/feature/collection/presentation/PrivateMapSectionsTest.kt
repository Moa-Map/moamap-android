package com.moamap.app.feature.collection.presentation

import com.moamap.app.feature.collection.domain.model.MyMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateMapSectionsTest {

    private fun myMap(id: Long, title: String, personal: Boolean) = MyMap(
        id = id,
        title = title,
        imageUrl = null,
        memberCount = 1,
        placeCount = 0,
        official = false,
        personal = personal,
    )

    @Test
    fun `개인 지도만 앞 묶음에 간다`() {
        val sections = listOf(
            myMap(1L, "내 지도", personal = true),
            myMap(2L, "성수 카페 투어", personal = false),
        ).splitPersonal()

        assertEquals(listOf("내 지도"), sections.personal.map { it.title })
    }

    /** 같은 카드가 한 화면에 두 번 뜨면 지도가 두 개인 줄 안다. */
    @Test
    fun `앞 묶음에 간 지도는 뒤 묶음에 없다`() {
        val sections = listOf(
            myMap(1L, "내 지도", personal = true),
            myMap(2L, "성수 카페 투어", personal = false),
        ).splitPersonal()

        assertEquals(listOf("성수 카페 투어"), sections.others.map { it.title })
    }

    @Test
    fun `개인 지도가 없으면 앞 묶음이 비고 나머지는 전부 뒤에 있다`() {
        val sections = listOf(
            myMap(1L, "성수 카페 투어", personal = false),
            myMap(2L, "주말 데이트", personal = false),
        ).splitPersonal()

        assertTrue(sections.personal.isEmpty())
        assertEquals(2, sections.others.size)
    }

    @Test
    fun `빈 목록이면 양쪽 다 빈다`() {
        val sections = emptyList<MyMap>().splitPersonal()

        assertTrue(sections.personal.isEmpty())
        assertTrue(sections.others.isEmpty())
    }

    /** 서버가 준 정렬을 뒤집지 않는다. */
    @Test
    fun `원래 순서를 유지한다`() {
        val sections = listOf(
            myMap(1L, "가", personal = false),
            myMap(2L, "나", personal = true),
            myMap(3L, "다", personal = false),
            myMap(4L, "라", personal = true),
        ).splitPersonal()

        assertEquals(listOf("나", "라"), sections.personal.map { it.title })
        assertEquals(listOf("가", "다"), sections.others.map { it.title })
    }
}
