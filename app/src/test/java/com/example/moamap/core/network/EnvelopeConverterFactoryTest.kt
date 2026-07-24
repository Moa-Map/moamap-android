package com.example.moamap.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

@Serializable
private data class Payload(val id: Long, val name: String)

private interface TestService {
    @GET("payload")
    suspend fun payload(): Payload

    @GET("payloads")
    suspend fun payloads(): List<Payload>

    @GET("nothing")
    suspend fun nothing()
}

class EnvelopeConverterFactoryTest {

    private lateinit var server: MockWebServer
    private lateinit var service: TestService

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(EnvelopeConverterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TestService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `단일 객체 응답에서 data만 꺼낸다`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"success":true,"data":{"id":7,"name":"모아맵"},"error":null}""")
        )

        val result = service.payload()

        assertEquals(7L, result.id)
        assertEquals("모아맵", result.name)
    }

    @Test
    fun `배열 응답도 data만 꺼낸다`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"success":true,"data":[{"id":1,"name":"a"},{"id":2,"name":"b"}]}""")
        )

        val result = service.payloads()

        assertEquals(2, result.size)
        assertEquals("b", result[1].name)
    }

    @Test
    fun `본문 없는 성공 응답은 Unit으로 통과한다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true}"""))

        service.nothing() // 예외가 나지 않으면 통과
    }

    @Test
    fun `200인데 success가 false면 ApiException을 던진다`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"success":false,"error":{"code":"PLACE_001","message":"없는 장소","status":404}}""")
        )

        try {
            service.payload()
            fail("ApiException 이 발생해야 한다")
        } catch (e: ApiException) {
            assertEquals("PLACE_001", e.code)
            assertEquals(404, e.status)
        }
    }

    @Test
    fun `성공인데 data가 비면 ApiException을 던진다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":null}"""))

        try {
            service.payload()
            fail("ApiException 이 발생해야 한다")
        } catch (e: ApiException) {
            assertEquals("EMPTY_DATA", e.code)
        }
    }
}
