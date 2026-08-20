package com.moamap.app.feature.officialmap.data.remote

import com.moamap.app.core.network.EnvelopeConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class FootTrafficServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: FootTrafficService

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
            .create(FootTrafficService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `지역 조회는 게이트웨이 경로를 호출한다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":[]}"""))

        service.getAreas()

        assertEquals("/api/v1/maps/official/foot-traffic/areas", server.takeRequest().path)
    }

    @Test
    fun `혼잡도 조회는 게이트웨이 경로를 호출한다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":[]}"""))

        service.getCongestions()

        assertEquals("/api/v1/maps/official/foot-traffic/congestion", server.takeRequest().path)
    }

    @Test
    fun `envelope에 감싸인 지역 응답을 DTO로 받는다`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"success":true,"data":[{"footTrafficAreaCd":"POI038","areaNm":"신도림역","lat":37.509021,"lng":126.890130,
                   "boundary":{"type":"Polygon","coordinates":[[[126.892284,37.509064]]]}}]}"""
            )
        )

        val areas = service.getAreas()

        assertEquals(1, areas.size)
        assertEquals("POI038", areas[0].footTrafficAreaCd)
        assertEquals("신도림역", areas[0].areaNm)
    }
}
