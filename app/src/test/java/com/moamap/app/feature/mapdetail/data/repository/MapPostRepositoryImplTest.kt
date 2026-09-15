package com.moamap.app.feature.mapdetail.data.repository

import android.net.Uri
import com.moamap.app.core.common.upload.PhotoSpec
import com.moamap.app.core.common.upload.PhotoUploader
import com.moamap.app.core.network.model.PageResponse
import com.moamap.app.feature.mapdetail.data.remote.MapPostCreateRequestDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostPhotoUploadUrlDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostPhotoUploadUrlRequestDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostPlaceTagRequestDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostService
import com.moamap.app.feature.mapdetail.domain.model.MapPostPlaceTag
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import com.moamap.app.feature.mapdetail.domain.model.NewMapPost
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private data class PostsCall(val mapId: Long, val page: Int?, val size: Int?, val sort: String?)

private class FakeMapPostService(
    private val response: PageResponse<MapPostDto> = PageResponse(),
) : MapPostService {

    val calls = mutableListOf<PostsCall>()
    val created = mutableListOf<Pair<Long, MapPostCreateRequestDto>>()

    override suspend fun getPosts(
        mapId: Long,
        page: Int?,
        size: Int?,
        sort: String?,
    ): PageResponse<MapPostDto> {
        calls += PostsCall(mapId, page, size, sort)
        return response
    }

    override suspend fun createPost(mapId: Long, request: MapPostCreateRequestDto): MapPostDto {
        created += mapId to request
        return MapPostDto(id = 1)
    }

    /** 사진 없는 경로만 검증한다. 불리면 발급을 막지 못한 것이다. */
    override suspend fun createPhotoUploadUrl(
        mapId: Long,
        request: MapPostPhotoUploadUrlRequestDto,
    ): MapPostPhotoUploadUrlDto = TODO("사진이 없으면 발급하지 않는다")
}

/**
 * `Uri` 는 JVM 유닛 테스트에서 만들 수 없어 사진이 있는 경로는 다루지 않는다. 다른 저장소 테스트와 같다.
 */
private class NoPhotoUploader : PhotoUploader {
    override suspend fun inspect(uri: Uri) = TODO("사진 없는 경로만 검증한다")
    override suspend fun upload(uploadUrl: String, photo: PhotoSpec) =
        TODO("사진 없는 경로만 검증한다")
}

private fun repository(service: MapPostService) = MapPostRepositoryImpl(service, NoPhotoUploader())

class MapPostRepositoryImplTest {

    @Test
    fun `지도와 페이지와 한 번에 받을 수를 넘긴다`() = runTest {
        val service = FakeMapPostService()

        repository(service).getPosts(mapId = 10, page = 2, sort = MapPostSort.Latest)

        val call = service.calls.single()
        assertEquals(10L, call.mapId)
        assertEquals(2, call.page)
        assertEquals(POST_PAGE_SIZE, call.size)
    }

    /** 최신순도 빼지 않고 보낸다. 서버 기본값이 바뀌어도 화면의 선택과 어긋나지 않는다. */
    @Test
    fun `정렬을 서버 형식으로 바꿔 보낸다`() = runTest {
        val service = FakeMapPostService()
        val repository = repository(service)

        repository.getPosts(mapId = 10, page = 0, sort = MapPostSort.Latest)
        repository.getPosts(mapId = 10, page = 0, sort = MapPostSort.Oldest)

        assertEquals(listOf("createdAt,desc", "createdAt,asc"), service.calls.map { it.sort })
    }

    @Test
    fun `응답을 도메인으로 옮기고 마지막 페이지 여부를 넘긴다`() = runTest {
        val service = FakeMapPostService(
            PageResponse(content = listOf(MapPostDto(id = 1), MapPostDto(id = 2)), last = false),
        )

        val page = repository(service).getPosts(mapId = 10, page = 0, sort = MapPostSort.Latest)

        assertEquals(listOf(1L, 2L), page.posts.map { it.id })
        assertFalse(page.isLast)
    }

    /** 서버가 `last` 를 잘못 내려도 스크롤할 때마다 헛조회가 반복되지 않게 한다. */
    @Test
    fun `빈 페이지는 마지막으로 본다`() = runTest {
        val service = FakeMapPostService(PageResponse(content = emptyList(), last = false))

        val page = repository(service).getPosts(mapId = 10, page = 3, sort = MapPostSort.Latest)

        assertTrue(page.isLast)
    }

    /** 발급 서비스는 불리면 터진다. 그게 이 테스트의 검증이다. */
    @Test
    fun `사진을 붙이지 않았으면 발급을 부르지 않는다`() = runTest {
        val urls = repository(FakeMapPostService()).uploadPhotos(mapId = 10, photos = emptyList())

        assertTrue(urls.isEmpty())
    }

    @Test
    fun `게시물 작성 요청에 본문과 사진 주소와 장소 태그를 담는다`() = runTest {
        val service = FakeMapPostService()

        repository(service).createPost(
            mapId = 10,
            post = NewMapPost(
                content = "성수 카페 다녀왔어요",
                photoUrls = listOf("https://cdn/1.jpg", "https://cdn/2.jpg"),
                placeTags = listOf(MapPostPlaceTag(placeId = 5, name = "블루보틀 성수점")),
            ),
        )

        val (mapId, request) = service.created.single()
        assertEquals(10L, mapId)
        assertEquals("성수 카페 다녀왔어요", request.content)
        assertEquals(listOf("https://cdn/1.jpg", "https://cdn/2.jpg"), request.imageUrls)
        assertEquals(listOf(MapPostPlaceTagRequestDto(placeId = 5, name = "블루보틀 성수점")), request.placeTags)
    }
}
