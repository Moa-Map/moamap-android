package com.example.moamap.feature.officialmap.data.remote

import com.example.moamap.core.network.EnvelopeConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class OfficialMapServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: OfficialMapService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(EnvelopeConverterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OfficialMapService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueEmptyPage() {
        server.enqueue(
            MockResponse().setBody(
                """{"success":true,"data":{"content":[],"page":0,"size":20,
                   "totalElements":0,"totalPages":0,"last":true}}"""
            )
        )
    }

    @Test
    fun `공식지도 목록은 게이트웨이 경로를 호출한다`() = runTest {
        enqueueEmptyPage()

        service.getOfficialMaps()

        assertEquals("/api/v1/maps/official", server.takeRequest().path)
    }

    @Test
    fun `넘긴 페이지 파라미터만 쿼리에 붙는다`() = runTest {
        enqueueEmptyPage()

        service.getOfficialMaps(size = 20)

        val path = server.takeRequest().path
        assertEquals("/api/v1/maps/official?size=20", path)
        // 넘기지 않은 값은 빈 파라미터로도 나가지 않는다.
        assertTrue(path?.contains("sort") == false)
    }

    @Test
    fun `envelope과 page를 벗겨 DTO 목록으로 받는다`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"success":true,"data":{"content":[
                   {"id":6,"name":"화장실 위치","description":"공공데이터 기반 공중화장실 위치",
                    "imageUrl":null,"type":"OFFICIAL","tags":[],"memberCount":1,
                    "joined":false,"placeCount":5416,"personal":false}],
                   "page":0,"size":20,"totalElements":1,"totalPages":1,"last":true}}"""
            )
        )

        val page = service.getOfficialMaps()

        assertEquals(1, page.content.size)
        assertEquals(6L, page.content[0].id)
        assertEquals("화장실 위치", page.content[0].name)
        assertEquals(5416, page.content[0].placeCount)
    }
}
