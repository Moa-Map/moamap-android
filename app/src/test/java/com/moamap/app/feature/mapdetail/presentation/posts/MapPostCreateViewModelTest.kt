package com.moamap.app.feature.mapdetail.presentation.posts

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.moamap.app.core.common.upload.ImageUploadException
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.domain.model.MapPostPage
import com.moamap.app.feature.mapdetail.domain.model.MapPostPlaceTag
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import com.moamap.app.feature.mapdetail.domain.model.NewMapPost
import com.moamap.app.feature.mapdetail.domain.repository.MapPostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private fun place(id: Long, name: String = "장소$id") = MapPlace(
    id = id,
    name = name,
    address = "서울 성동구 $id",
    latitude = 0.0,
    longitude = 0.0,
    photoUrl = null,
)

/**
 * `Uri` 는 JVM 유닛 테스트에서 만들 수 없어 사진을 고르는 경로는 다루지 않는다. 장소 추가 테스트와 같다.
 * 대신 업로드가 돌려주는 주소를 바꿔 가며 "올린 뒤 작성" 순서와 재시도를 확인한다.
 */
private class FakeCreateRepository : MapPostRepository {

    val calls = mutableListOf<String>()
    val created = mutableListOf<NewMapPost>()

    var uploadResult: List<String> = emptyList()
    var uploadError: Exception? = null
    var createError: Exception? = null

    override suspend fun getPosts(mapId: Long, page: Int, sort: MapPostSort): MapPostPage =
        TODO("작성 화면은 목록을 읽지 않는다")

    override suspend fun uploadPhotos(mapId: Long, photos: List<Uri>): List<String> {
        calls += "uploadPhotos"
        uploadError?.let { throw it }
        return uploadResult
    }

    override suspend fun createPost(mapId: Long, post: NewMapPost) {
        calls += "createPost"
        createError?.let { throw it }
        created += post
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MapPostCreateViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: MapPostRepository = FakeCreateRepository()) = MapPostCreateViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to 7L)),
        repository = repository,
    )

    @Test
    fun `본문이 비어 있으면 올릴 수 없다`() {
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.updateContent("   ")
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.updateContent("성수 카페")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    /** 붙여 넣기로 한 번에 넘길 수 있다. 서버는 1000자를 넘으면 거절한다. */
    @Test
    fun `본문은 한도까지만 받는다`() {
        val viewModel = viewModel()

        viewModel.updateContent("가".repeat(MAX_POST_CONTENT_LENGTH + 5))

        assertEquals(MAX_POST_CONTENT_LENGTH, viewModel.uiState.value.content.length)
    }

    @Test
    fun `장소는 한 곳만 태그하고 빼면 다시 고를 수 있다`() {
        val viewModel = viewModel()

        viewModel.addPlace(place(1))
        // 한 곳을 고른 뒤로는 화면에서 「장소 추가」 줄이 사라진다.
        assertFalse(viewModel.uiState.value.canAddPlace)

        viewModel.addPlace(place(2))
        assertEquals(listOf(1L), viewModel.uiState.value.places.map { it.placeId })

        viewModel.removePlace(1)
        assertTrue(viewModel.uiState.value.places.isEmpty())
        assertTrue(viewModel.uiState.value.canAddPlace)

        viewModel.addPlace(place(2))
        assertEquals(listOf(2L), viewModel.uiState.value.places.map { it.placeId })
    }

    @Test
    fun `사진을 먼저 올리고 받은 주소로 게시물을 올린다`() = runTest {
        val repository = FakeCreateRepository().apply { uploadResult = listOf("https://cdn/1.jpg") }
        val viewModel = viewModel(repository).apply {
            updateContent("  성수 카페 다녀왔어요  ")
            addPlace(place(5, name = "블루보틀 성수점"))
        }

        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("uploadPhotos", "createPost"), repository.calls)
        val post = repository.created.single()
        assertEquals("성수 카페 다녀왔어요", post.content)
        assertEquals(listOf("https://cdn/1.jpg"), post.photoUrls)
        assertEquals(listOf(MapPostPlaceTag(placeId = 5, name = "블루보틀 성수점")), post.placeTags)
    }

    @Test
    fun `올리고 나면 폼을 비우고 성공을 알린다`() = runTest {
        val viewModel = viewModel().apply {
            updateContent("글")
            addPlace(place(1))
        }

        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.postedCount)
        assertEquals("", state.content)
        assertTrue(state.places.isEmpty())
        assertFalse(state.submitting)
    }

    /** 형식·크기 때문에 걸린 것은 사용자가 사진을 바꿔야 한다. 이유를 그대로 보여준다. */
    @Test
    fun `사진이 걸리면 이유를 보여주고 게시물은 올리지 않는다`() = runTest {
        val repository = FakeCreateRepository().apply { uploadError = ImageUploadException.UnsupportedType() }
        val viewModel = viewModel(repository).apply { updateContent("글") }

        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("uploadPhotos"), repository.calls)
        assertEquals(ImageUploadException.UnsupportedType().message, viewModel.uiState.value.errorMessage)
        assertEquals("글", viewModel.uiState.value.content)
    }

    /** 매번 새로 올리면 지울 수 없는 사진이 시도할 때마다 쌓인다. */
    @Test
    fun `작성만 실패했으면 다시 누를 때 사진을 또 올리지 않는다`() = runTest {
        val repository = FakeCreateRepository().apply {
            uploadResult = listOf("https://cdn/1.jpg")
            createError = RuntimeException("boom")
        }
        val viewModel = viewModel(repository).apply { updateContent("글") }

        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(POST_CREATE_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)
        assertEquals("글", viewModel.uiState.value.content)

        repository.createError = null
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("uploadPhotos", "createPost", "createPost"), repository.calls)
        assertEquals(listOf("https://cdn/1.jpg"), repository.created.single().photoUrls)
    }

    /** 성공 횟수를 되돌리면 화면이 "새로 성공했다" 는 신호를 놓친다. */
    @Test
    fun `다시 열어 비워도 성공 횟수는 남긴다`() = runTest {
        val viewModel = viewModel().apply { updateContent("글") }
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateContent("쓰다 만 글")
        viewModel.reset()

        assertEquals(1, viewModel.uiState.value.postedCount)
        assertEquals("", viewModel.uiState.value.content)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
