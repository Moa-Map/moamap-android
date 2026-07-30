package com.example.moamap.feature.mapdetail.data.repository

import com.example.moamap.feature.collection.data.remote.MapDetailDto
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.explore.data.remote.PlaceDto
import com.example.moamap.feature.mapdetail.domain.model.MapRole
import com.example.moamap.feature.mapdetail.domain.model.areaLabel
import com.example.moamap.feature.mapdetail.domain.model.categoryLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapDetailMapperTest {

    @Test
    fun `이름이 비면 자리를 채운다`() {
        assertEquals("이름 없는 지도", MapDetailDto(name = null).toMapDetail(null).title)
        assertEquals("이름 없는 지도", MapDetailDto(name = "  ").toMapDetail(null).title)
    }

    @Test
    fun `설명과 이미지가 공백이면 접는다`() {
        val map = MapDetailDto(description = "  ", imageUrl = "").toMapDetail(ownerName = " ")

        assertNull(map.description)
        assertNull(map.imageUrl)
        assertNull(map.ownerName)
    }

    @Test
    fun `PRIVATE 이 아닌 타입은 모두 커뮤니티로 본다`() {
        // 화면의 판단 기준은 "프라이빗이냐" 하나뿐이다. 공식 지도는 이 화면으로 오지 않는다.
        assertEquals(MapType.Private, MapDetailDto(type = "PRIVATE").toMapDetail(null).type)
        assertEquals(MapType.Community, MapDetailDto(type = "COMMUNITY").toMapDetail(null).type)
        assertEquals(MapType.Community, MapDetailDto(type = "OFFICIAL").toMapDetail(null).type)
        assertEquals(MapType.Community, MapDetailDto(type = null).toMapDetail(null).type)
    }

    @Test
    fun `모르는 역할은 권한 없음으로 옮긴다`() {
        assertEquals(MapRole.Owner, MapDetailDto(myRole = "OWNER").toMapDetail(null).role)
        assertEquals(MapRole.None, MapDetailDto(myRole = "GUEST").toMapDetail(null).role)
    }

    @Test
    fun `공백뿐인 태그는 걸러낸다`() {
        val map = MapDetailDto(tags = listOf("카페", " ", "데이트")).toMapDetail(null)

        assertEquals(listOf("카페", "데이트"), map.tags)
    }

    @Test
    fun `참여 인원과 장소 수를 그대로 옮긴다`() {
        val map = MapDetailDto(memberCount = 12, placeCount = 32, joined = true).toMapDetail(null)

        assertEquals(12, map.memberCount)
        assertEquals(32, map.placeCount)
        assertEquals(true, map.joined)
    }

    @Test
    fun `나만의 지도 표시를 그대로 옮긴다`() {
        // PRIVATE 타입으로 내려오므로 type 으로는 못 가린다. 이 값만 보고 판단한다.
        assertEquals(true, MapDetailDto(type = "PRIVATE", personal = true).toMapDetail(null).personal)
        assertEquals(false, MapDetailDto(type = "PRIVATE").toMapDetail(null).personal)
    }

    @Test
    fun `장소 주소는 도로명을 우선한다`() {
        val place = PlaceDto(
            address = "서울 성동구 성수동1가 1",
            roadAddress = "서울 성동구 성수이로 12",
        ).toMapPlace()

        assertEquals("서울 성동구 성수이로 12", place.address)
    }

    @Test
    fun `도로명이 없으면 지번 주소로 대신한다`() {
        val place = PlaceDto(address = "서울 성동구 성수동1가 1", roadAddress = " ").toMapPlace()

        assertEquals("서울 성동구 성수동1가 1", place.address)
    }

    @Test
    fun `주소가 둘 다 없으면 빈 문자열이다`() {
        assertEquals("", PlaceDto().toMapPlace().address)
    }

    @Test
    fun `장소 사진은 공백이 아닌 첫 장을 쓴다`() {
        assertNull(PlaceDto(photoUrls = emptyList()).toMapPlace().photoUrl)
        assertNull(PlaceDto(photoUrls = listOf(" ")).toMapPlace().photoUrl)
        assertEquals(
            "https://img/2.jpg",
            PlaceDto(photoUrls = listOf("", "https://img/2.jpg")).toMapPlace().photoUrl,
        )
    }

    @Test
    fun `이름 없는 장소도 자리를 채운다`() {
        assertEquals("이름 없는 장소", PlaceDto(name = "").toMapPlace().name)
    }

    @Test
    fun `평점과 댓글 수를 목록이 읽을 값으로 옮긴다`() {
        val place = PlaceDto(avgRating = 4.8, commentCount = 124).toMapPlace()

        assertEquals(4.8, place.rating, 1e-9)
        assertEquals(124, place.reviewCount)
    }

    @Test
    fun `아무도 평점을 안 매겼으면 0이다`() {
        // avgRating 이 null 로 온다. 목록은 숫자를 그려야 하니 0.0 으로 읽는다.
        assertEquals(0.0, PlaceDto(avgRating = null).toMapPlace().rating, 1e-9)
    }

    @Test
    fun `설명과 분류가 비면 빈 문자열이다`() {
        val place = PlaceDto(description = " ", category = null).toMapPlace()

        assertEquals("", place.description)
        assertEquals("", place.category)
    }

    @Test
    fun `분류 경로에서 마지막 토막만 띄운다`() {
        val place = PlaceDto(category = "음식점 > 카페 > 커피전문점").toMapPlace()

        // 경로를 통째로 띄우면 상세 시트의 작은 알약을 넘겨 버린다.
        assertEquals("커피전문점", place.categoryLabel)
    }

    @Test
    fun `분류가 한 토막이면 그대로 띄운다`() {
        assertEquals("관광명소", PlaceDto(category = "관광명소").toMapPlace().categoryLabel)
    }

    @Test
    fun `분류가 없으면 라벨도 빈 문자열이다`() {
        assertEquals("", PlaceDto(category = null).toMapPlace().categoryLabel)
    }

    @Test
    fun `주소에서 지역 한 토막을 꺼낸다`() {
        val place = PlaceDto(roadAddress = "서울 성동구 성수이로 12").toMapPlace()

        assertEquals("성동구", place.areaLabel)
    }

    @Test
    fun `주소가 없으면 지역도 빈 문자열이다`() {
        assertEquals("", PlaceDto().toMapPlace().areaLabel)
    }
}
