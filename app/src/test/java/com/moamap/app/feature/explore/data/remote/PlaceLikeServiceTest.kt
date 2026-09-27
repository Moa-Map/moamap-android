package com.moamap.app.feature.explore.data.remote

import com.moamap.app.core.network.EnvelopeConverterFactory
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

/** 하트 누르기·취소 경로와, 서버가 쓰는 필드 이름(`liked`, `likedByMe`)을 지킨다. */
class PlaceLikeServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: PlaceService

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
            .create(PlaceService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueData(data: String) {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":$data}"""))
    }

    @Test
    fun `하트 누르기는 POST 로 보내고 누른 뒤 상태를 받는다`() = runTest {
        enqueueData("""{"placeId":7,"likeCount":12,"liked":true}""")

        val like = service.likePlace(7)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/places/7/likes", request.path)
        assertTrue(like.liked)
        assertEquals(12, like.likeCount)
    }

    @Test
    fun `하트 취소는 같은 경로에 DELETE 로 보낸다`() = runTest {
        enqueueData("""{"placeId":7,"likeCount":11,"liked":false}""")

        service.unlikePlace(7)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/places/7/likes", request.path)
    }

    @Test
    fun `장소 응답에서 하트 수와 내가 눌렀는지를 읽는다`() = runTest {
        enqueueData("""{"id":7,"name":"카페","likeCount":3,"likedByMe":true}""")

        val place = service.getPlace(7)

        assertEquals(3, place.likeCount)
        assertTrue(place.likedByMe)
    }
}
