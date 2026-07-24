package com.example.moamap.core.network.di

import com.example.moamap.core.network.ApiException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.http.GET

@Serializable
private data class Area(val code: String)

private interface AreaService {
    @GET("api/v1/areas")
    suspend fun areas(): List<Area>
}

/**
 * NetworkModule 이 실제로 조립한 클라이언트가 계약대로 동작하는지 확인한다.
 * baseUrl 만 MockWebServer 로 바꿔 끼운다.
 */
class NetworkModuleTest {

    private lateinit var server: MockWebServer
    private lateinit var service: AreaService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val json = NetworkModule.provideJson()
        val client = NetworkModule.provideOkHttpClient(json)
        val retrofit = NetworkModule.provideRetrofit(json, client)
            .newBuilder()
            .baseUrl(server.url("/"))
            .build()
        service = retrofit.create(AreaService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `조립된 클라이언트가 envelope을 벗겨 페이로드를 준다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":[{"code":"POI001"}]}"""))

        val result = service.areas()

        assertEquals(1, result.size)
        assertEquals("POI001", result[0].code)
    }

    @Test
    fun `조립된 클라이언트가 실패 응답을 ApiException으로 던진다`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"success":false,"error":{"code":"COMMON_005","message":"인증이 필요합니다.","status":401}}""")
        )

        try {
            service.areas()
            fail("ApiException 이 발생해야 한다")
        } catch (e: ApiException) {
            assertEquals("COMMON_005", e.code)
            assertEquals(401, e.status)
        }
    }

    @Test
    fun `기본 Retrofit의 baseUrl은 BuildConfig 값을 쓴다`() {
        val json = NetworkModule.provideJson()
        val retrofit: Retrofit = NetworkModule.provideRetrofit(
            json,
            NetworkModule.provideOkHttpClient(json),
        )

        assertEquals(com.example.moamap.BuildConfig.BASE_URL, retrofit.baseUrl().toString())
    }
}
