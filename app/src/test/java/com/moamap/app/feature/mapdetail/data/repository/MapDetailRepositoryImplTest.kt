package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.EnvelopeConverterFactory
import com.moamap.app.core.network.interceptor.ErrorInterceptor
import com.moamap.app.feature.collection.data.remote.MapService
import com.moamap.app.feature.explore.data.remote.PlaceService
import com.moamap.app.feature.mypage.data.remote.UserService
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import com.moamap.app.feature.mapdetail.domain.model.ReportReason
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class MapDetailRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: MapDetailRepositoryImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            // 앱과 같이 실패 응답을 ApiException 으로 바꾼다.
            .client(OkHttpClient.Builder().addInterceptor(ErrorInterceptor(json)).build())
            .addConverterFactory(EnvelopeConverterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = MapDetailRepositoryImpl(
            mapService = retrofit.create(MapService::class.java),
            placeService = retrofit.create(PlaceService::class.java),
            userService = retrofit.create(UserService::class.java),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** 정렬을 안 보내면 서버가 순서를 정하지 않아 열 때마다 목록 순서가 바뀔 수 있다(이슈 #312). */
    @Test
    fun `장소 목록은 최신순으로 받는다`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"success":true,"data":{"content":[],"last":true}}"""),
        )

        repository.getPlaces(mapId = 6)

        val url = server.takeRequest().requestUrl!!
        assertEquals("id,desc", url.queryParameter("sort"))
        assertEquals("6", url.queryParameter("mapId"))
    }

    /** 다른 기기나 화면에서 먼저 참여했을 때 온다. 실패로 알리면 지도로 넘어가지 못한다(이슈 #316). */
    @Test
    fun `이미 참여한 지도라는 답은 참여 성공으로 본다`() = runTest {
        server.enqueue(errorResponse(status = 409, code = "MAP_005", message = "이미 참여한 지도입니다."))

        repository.joinMap(mapId = 4)

        assertEquals("/api/v1/maps/4/join", server.takeRequest().path)
    }

    @Test
    fun `참여의 다른 실패는 그대로 알린다`() = runTest {
        server.enqueue(errorResponse(status = 404, code = "MAP_001", message = "지도를 찾을 수 없습니다."))

        val error = runCatching { repository.joinMap(mapId = 4) }.exceptionOrNull()

        assertEquals("MAP_001", (error as ApiException).code)
    }

    /** 서버는 null 칸을 그대로 두므로, 비운 메모·태그도 빈 값으로 실어야 지워진다(이슈 #376). */
    @Test
    fun `장소 수정은 사진을 안 바꾸면 메모·태그만 싣고 비운 값도 보낸다`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"success":true,"data":{"id":5,"name":"카페","description":"","tags":[]}}"""),
        )

        val updated = repository.updatePlace(placeId = 5, memo = "", tags = emptyList(), photoUrls = null)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/places/5", request.path)
        // 사진 칸이 빠져야 서버가 사진을 그대로 둔다.
        assertEquals("""{"description":"","tags":[]}""", request.body.readUtf8())
        assertEquals(5L, updated.id)
    }

    /** 서버가 사진을 통째로 바꿔, 지운 사진은 빈 목록으로 가야 지워진다(이슈 #398). */
    @Test
    fun `장소 수정은 바꾼 사진을 전체 목록으로 싣는다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"id":5,"name":"카페"}}"""))

        repository.updatePlace(placeId = 5, memo = "메모", tags = emptyList(), photoUrls = emptyList())

        assertEquals(
            """{"description":"메모","tags":[],"photoUrls":[]}""",
            server.takeRequest().body.readUtf8(),
        )
    }

    @Test
    fun `장소 신고는 장소 아래 reports 로 사유와 공백 지운 내용을 보낸다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":null}"""))

        repository.reportPlace(placeId = 5, reason = ReportReason.Other, detail = " 사진이 달라요 ")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/places/5/reports", request.path)
        assertEquals("""{"reason":"OTHER","detail":"사진이 달라요"}""", request.body.readUtf8())
    }

    @Test
    fun `장소 삭제는 그 장소로 DELETE 를 보낸다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":null}"""))

        repository.deletePlace(placeId = 5)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/places/5", request.path)
    }

    private fun errorResponse(status: Int, code: String, message: String) = MockResponse()
        .setResponseCode(status)
        .setBody("""{"success":false,"error":{"code":"$code","message":"$message","status":$status}}""")
}
