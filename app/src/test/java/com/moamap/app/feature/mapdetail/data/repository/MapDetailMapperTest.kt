package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.collection.data.remote.MapDetailDto
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.explore.data.remote.PlaceDto
import com.moamap.app.feature.mapdetail.domain.model.MapRole
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
    fun `서버 타입을 그대로 옮긴다`() {
        assertEquals(MapType.Private, MapDetailDto(type = "PRIVATE").toMapDetail(null).type)
        assertEquals(MapType.Community, MapDetailDto(type = "COMMUNITY").toMapDetail(null).type)
        assertEquals(MapType.Official, MapDetailDto(type = "OFFICIAL").toMapDetail(null).type)
    }

    @Test
    fun `모르는 타입은 커뮤니티로 본다`() {
        // 서버가 종류를 하나 더 늘려도 화면은 열려야 한다.
        assertEquals(MapType.Community, MapDetailDto(type = null).toMapDetail(null).type)
        assertEquals(MapType.Community, MapDetailDto(type = "SOMETHING").toMapDetail(null).type)
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
    fun `초대 코드는 공백이면 접는다`() {
        // 빈 코드는 없는 것과 같다. 화면이 코드 없음으로 한 번에 판단할 수 있어야 한다.
        assertEquals("VH4YXZ", MapDetailDto(inviteCode = "VH4YXZ").toMapDetail(null).inviteCode)
        assertNull(MapDetailDto(inviteCode = " ").toMapDetail(null).inviteCode)
        assertNull(MapDetailDto().toMapDetail(null).inviteCode)
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
    fun `태그는 앞뒤 공백을 걷고 빈 태그는 버린다`() {
        val place = PlaceDto(tags = listOf(" 카페 ", "", "  ", "성수")).toMapPlace()

        assertEquals(listOf("카페", "성수"), place.tags)
    }

    @Test
    fun `인스타그램에서 가져온 장소만 원본 주소를 싣는다`() {
        val reel = "https://www.instagram.com/reel/abc"

        assertEquals(reel, PlaceDto(sourceType = "INSTAGRAM", sourceUrl = " $reel ").toMapPlace().instagramUrl)
        // 카카오 검색으로 등록한 장소의 sourceUrl 은 카카오맵 주소다. 인스타그램 버튼을 띄우면 안 된다.
        assertNull(PlaceDto(sourceType = "KAKAO_SEARCH", sourceUrl = "https://place.map.kakao.com/1").toMapPlace().instagramUrl)
        assertNull(PlaceDto(sourceType = "INSTAGRAM", sourceUrl = null).toMapPlace().instagramUrl)
        assertNull(PlaceDto(sourceType = "INSTAGRAM", sourceUrl = " ").toMapPlace().instagramUrl)
    }

    @Test
    fun `웹 주소가 아니면 인스타그램 원본으로 열지 않는다`() {
        // 화면이 이 주소를 그대로 열어, 다른 앱을 띄우는 형식이 섞여 오면 막아야 한다.
        assertNull(instagramSourceUrl("INSTAGRAM", "intent://scan#Intent;scheme=zxing;end"))
        assertNull(instagramSourceUrl("INSTAGRAM", "javascript:alert(1)"))
        assertEquals("HTTP://instagram.com/p/1", instagramSourceUrl("INSTAGRAM", "HTTP://instagram.com/p/1"))
    }

    @Test
    fun `카카오 장소 id 는 공백을 걷어 옮기고 없으면 빈 문자열이다`() {
        assertEquals("76206032", PlaceDto(kakaoPlaceId = " 76206032 ").toMapPlace().kakaoPlaceId)
        assertEquals("", PlaceDto(kakaoPlaceId = null).toMapPlace().kakaoPlaceId)
    }

    @Test
    fun `하트 수와 내가 눌렀는지를 옮긴다`() {
        val place = PlaceDto(likeCount = 12, likedByMe = true).toMapPlace()

        assertEquals(12, place.likeCount)
        assertEquals(true, place.liked)
        // 서버가 필드를 빼고 보내면 누르지 않은 것으로 본다.
        assertEquals(false, PlaceDto().toMapPlace().liked)
    }
}
