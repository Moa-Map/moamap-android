package com.example.moamap.core.network.interceptor

import com.example.moamap.core.network.ApiException
import com.example.moamap.core.network.ConnectionException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ErrorInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(ErrorInterceptor(json))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun call() = client.newCall(Request.Builder().url(server.url("/api/v1/users/me")).build())

    @Test
    fun `실패 응답은 서버 코드를 담은 ApiException으로 바뀐다`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"success":false,"error":{"code":"COMMON_007","message":"토큰이 만료되었습니다.","status":401}}""")
        )

        try {
            call().execute()
            fail("ApiException 이 발생해야 한다")
        } catch (e: ApiException) {
            assertEquals("COMMON_007", e.code)
            assertEquals(401, e.status)
            assertEquals("토큰이 만료되었습니다.", e.serverMessage)
        }
    }

    @Test
    fun `에러 본문을 파싱할 수 없으면 HTTP 상태로 대체한다`() {
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>Bad Gateway</html>"))

        try {
            call().execute()
            fail("ApiException 이 발생해야 한다")
        } catch (e: ApiException) {
            assertEquals("UNKNOWN", e.code)
            assertEquals(502, e.status)
        }
    }

    @Test
    fun `본문이 비어 있어도 ApiException을 던진다`() {
        server.enqueue(MockResponse().setResponseCode(404))

        try {
            call().execute()
            fail("ApiException 이 발생해야 한다")
        } catch (e: ApiException) {
            assertEquals("UNKNOWN", e.code)
            assertEquals(404, e.status)
        }
    }

    @Test
    fun `서버에 닿지 못하면 ConnectionException으로 바뀐다`() {
        server.shutdown() // 포트를 닫아 연결 실패를 만든다

        try {
            call().execute()
            fail("ConnectionException 이 발생해야 한다")
        } catch (e: ConnectionException) {
            assertTrue(e.cause is java.io.IOException)
        }
    }

    @Test
    fun `성공 응답은 그대로 통과시킨다`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true,"data":null}"""))

        val response = call().execute()

        assertEquals(200, response.code)
        assertEquals("""{"success":true,"data":null}""", response.body?.string())
    }
}
