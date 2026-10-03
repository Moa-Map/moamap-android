package com.moamap.app.feature.officialmap.data.remote

import com.moamap.app.core.network.EnvelopeConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RestroomServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: RestroomService

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
            .create(RestroomService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `범위 조회는 남서·북동 좌표를 질의로 보내고 목록과 잘림 여부를 읽는다`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"success":true,"data":{"restrooms":[{"id":27205,"name":"시청역(1) 화장실",""" +
                    """"lat":37.565439,"lng":126.976983,"category":"공중화장실",""" +
                    """"disabledAccessible":true,"openHours":"정시"}],"truncated":true}}""",
            ),
        )

        val result = service.getRestrooms(swLat = 37.5, swLng = 126.9, neLat = 37.6, neLng = 127.0)

        val request = server.takeRequest()
        assertEquals("/api/v1/maps/official/restrooms", request.requestUrl?.encodedPath)
        assertEquals("37.5", request.requestUrl?.queryParameter("swLat"))
        assertEquals("126.9", request.requestUrl?.queryParameter("swLng"))
        assertEquals("37.6", request.requestUrl?.queryParameter("neLat"))
        assertEquals("127.0", request.requestUrl?.queryParameter("neLng"))
        assertEquals(27205L, result.restrooms.single().id)
        assertEquals("시청역(1) 화장실", result.restrooms.single().name)
        assertTrue(result.truncated)
    }

    @Test
    fun `상세 조회는 화장실 번호 경로를 부르고 비어 온 값은 null 로 읽는다`() = runTest {
        // 개발 서버 실제 응답(시청역(1) 화장실)에서 줄인 것
        server.enqueue(
            MockResponse().setBody(
                """{"success":true,"data":{"id":27205,"mngNo":"202430100000100165",""" +
                    """"name":"시청역(1) 화장실","category":"공중화장실","roadAddress":"서울특별시 중구 세종대로 지하 101",""" +
                    """"lotAddress":"서울특별시 중구 정동 5-5","maleToilet":6,"maleUrinal":5,"femaleToilet":15,""" +
                    """"openHours":"정시","openHoursDetail":"05:00~24:00","diaperTable":true,""" +
                    """"managerOrg":"서울교통공사","phone":"02-6110-1321","installedYm":null,"dataRefDate":"2024-12-02"}}""",
            ),
        )

        val detail = service.getRestroom(27205)

        assertEquals("/api/v1/maps/official/restrooms/27205", server.takeRequest().path)
        assertEquals(6, detail.maleToilet)
        assertEquals("05:00~24:00", detail.openHoursDetail)
        assertEquals(true, detail.diaperTable)
        assertNull(detail.emergencyBell)
        assertEquals("2024-12-02", detail.dataRefDate)
    }
}
