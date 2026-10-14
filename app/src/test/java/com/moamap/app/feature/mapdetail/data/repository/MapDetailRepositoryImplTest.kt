package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.core.network.EnvelopeConverterFactory
import com.moamap.app.feature.collection.data.remote.MapService
import com.moamap.app.feature.explore.data.remote.PlaceService
import com.moamap.app.feature.mypage.data.remote.UserService
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
}
