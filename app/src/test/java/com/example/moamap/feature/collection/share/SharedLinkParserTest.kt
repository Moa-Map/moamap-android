package com.example.moamap.feature.collection.share

import com.example.moamap.feature.collection.domain.model.PlaceImportSource
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedLinkParserTest {

    private fun supported(text: String?) = SharedLinkParser.parse(text) as SharedLink.Supported

    @Test
    fun `인스타그램 릴스 공유는 인스타그램 흐름으로 간다`() {
        val link = supported("https://www.instagram.com/reel/ABC123/?igsh=MXAzYnk1ZQ%3D%3D")

        assertEquals(PlaceImportSource.Instagram, link.source)
        assertEquals("https://www.instagram.com/reel/ABC123/?igsh=MXAzYnk1ZQ%3D%3D", link.url)
    }

    @Test
    fun `네이버 지도 단축 링크는 외부 지도 흐름으로 간다`() {
        // 네이버 지도는 앱 이름을 앞에 붙여 공유한다.
        val link = supported("네이버 지도\nhttps://naver.me/xAbCdEf")

        assertEquals(PlaceImportSource.MapShare, link.source)
        assertEquals("https://naver.me/xAbCdEf", link.url)
    }

    @Test
    fun `카카오맵 단축 링크는 외부 지도 흐름으로 간다`() {
        val link = supported("카카오맵에서 확인하세요 https://kko.to/0FyvknIfua")

        assertEquals(PlaceImportSource.MapShare, link.source)
        assertEquals("https://kko.to/0FyvknIfua", link.url)
    }

    @Test
    fun `구글 지도 단축 링크는 외부 지도 흐름으로 간다`() {
        val link = supported("성수동 카페\nhttps://maps.app.goo.gl/AbCdEfGhIjK")

        assertEquals(PlaceImportSource.MapShare, link.source)
        assertEquals("https://maps.app.goo.gl/AbCdEfGhIjK", link.url)
    }

    @Test
    fun `지도 도메인은 서브도메인까지 인정한다`() {
        val urls = listOf(
            "https://naver.me/xAbCdEf",
            "https://map.naver.com/p/entry/place/1234567",
            "https://m.map.naver.com/pt/1234567",
            "https://pages.map.naver.com/save-pages/web/detail-list/abc",
            "https://place.naver.com/restaurant/1234567/home",
            "https://m.place.naver.com/restaurant/1234567/home",
            "https://kko.to/0FyvknIfua",
            "https://kko.kakao.com/AbCdEfGh",
            "https://map.kakao.com/?target=other&folderid=23211144",
            // 카카오 단축링크가 풀리면 나오는 주소. 정확히 일치하는 목록으로 들면 놓친다.
            "https://applink.map.kakao.com/open?page=bookmark&folderid=1",
            "https://m.map.kakao.com/actions/detailMapView?id=1234567",
            "https://place.map.kakao.com/1234567",
            "https://maps.app.goo.gl/AbCdEfGhIjK",
            "https://maps.google.com/?cid=1234567",
            "https://goo.gl/maps/AbCdEfGhIjK",
            "https://www.google.com/maps/place/성수동",
            "https://google.co.kr/maps/place/성수동",
        )

        for (url in urls) {
            assertEquals(url, PlaceImportSource.MapShare, supported(url).source)
        }
    }

    @Test
    fun `지도 도메인을 흉내 낸 호스트는 거절한다`() {
        val urls = listOf(
            "https://map.naver.com.evil.com/path",
            "https://evil.com/map.kakao.com",
            "https://notgoo.gl/maps/abc",
        )

        for (url in urls) {
            assertEquals(url, SharedLink.Unsupported, SharedLinkParser.parse(url))
        }
    }

    @Test
    fun `한글이 섞인 주소도 판별한다`() {
        // java_net_URI 는 인코딩되지 않은 한글에서 예외를 던진다. 정규식으로 갈라야 하는 이유다.
        val link = supported("https://www.google.com/maps/place/서울숲/@37.5,127.0,17z")

        assertEquals(PlaceImportSource.MapShare, link.source)
    }

    @Test
    fun `구글 도메인이라도 지도 경로가 아니면 지원하지 않는다`() {
        assertEquals(SharedLink.Unsupported, SharedLinkParser.parse("https://www.google.com/search?q=성수동"))
    }

    @Test
    fun `URL 뒤에 붙은 문장부호는 떼어낸다`() {
        assertEquals("https://naver.me/xAbCdEf", supported("여기 어때? (https://naver.me/xAbCdEf)").url)
        assertEquals("https://naver.me/xAbCdEf", supported("여기야. https://naver.me/xAbCdEf.").url)
    }

    @Test
    fun `여러 URL 이 섞이면 첫 번째를 쓴다`() {
        val link = supported("https://naver.me/xAbCdEf 그리고 https://kko.kakao.com/AbCdEfGh")

        assertEquals("https://naver.me/xAbCdEf", link.url)
    }

    @Test
    fun `인스타그램 프로필 링크는 지원하지 않는다`() {
        assertEquals(
            SharedLink.Unsupported,
            SharedLinkParser.parse("https://www.instagram.com/moamap_official/"),
        )
    }

    @Test
    fun `지원하지 않는 호스트는 거절한다`() {
        assertEquals(SharedLink.Unsupported, SharedLinkParser.parse("https://youtu.be/AbCdEfGh"))
        assertEquals(SharedLink.Unsupported, SharedLinkParser.parse("https://example.com/place/1"))
    }

    @Test
    fun `URL 이 없는 텍스트는 거절한다`() {
        assertEquals(SharedLink.Unsupported, SharedLinkParser.parse("오늘 여기 갈래?"))
        assertEquals(SharedLink.Unsupported, SharedLinkParser.parse(""))
        assertEquals(SharedLink.Unsupported, SharedLinkParser.parse(null))
    }
}
