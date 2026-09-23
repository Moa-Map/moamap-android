package com.moamap.app.feature.explore.data.remote

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

/** 서버가 09-22 배포에서 `/reviews` 를 없애고 `/comments` 로 바꿨다. 옛 경로로 돌아가지 않게 막는다. */
class ReviewServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: ReviewService

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
            .create(ReviewService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueData(data: String) {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":$data}"""))
    }

    private fun enqueueComment() {
        enqueueData("""{"id":3,"placeId":7,"userId":1,"rating":5,"content":"좋아요","imageUrls":[]}""")
    }

    @Test
    fun `댓글 목록은 comments 경로를 호출한다`() = runTest {
        enqueueData("""{"content":[],"page":0,"size":100,"totalElements":0,"totalPages":0,"last":true}""")

        service.getReviews(placeId = 7, page = 0, size = 100)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/places/7/comments?page=0&size=100", request.path)
    }

    @Test
    fun `댓글 작성은 comments 경로를 호출한다`() = runTest {
        enqueueComment()

        service.createReview(placeId = 7, request = PlaceReviewCreateRequestDto(rating = 5, content = "좋아요"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/places/7/comments", request.path)
    }

    @Test
    fun `댓글 사진 주소 발급은 comments 아래 경로를 호출한다`() = runTest {
        enqueueData("""{"uploadUrl":"https://upload","fileUrl":"https://file","objectKey":"k","expiresInSeconds":600}""")

        service.createPhotoUploadUrl(
            placeId = 7,
            request = PlaceReviewPhotoUploadUrlRequestDto(contentType = "image/jpeg", fileSize = 1024),
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/places/7/comments/photo-upload-url", request.path)
    }

    @Test
    fun `댓글 수정은 comments 아래 댓글 id 경로를 호출한다`() = runTest {
        enqueueComment()

        service.updateReview(placeId = 7, reviewId = 3, request = PlaceReviewUpdateRequestDto(content = "수정"))

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/places/7/comments/3", request.path)
    }

    @Test
    fun `댓글 삭제는 comments 아래 댓글 id 경로를 호출한다`() = runTest {
        enqueueData("null")

        service.deleteReview(placeId = 7, reviewId = 3)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/places/7/comments/3", request.path)
    }
}
