package com.moamap.app.feature.mapdetail.presentation.posts

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.moamap.app.core.auth.FakeCurrentUserStore
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.feature.mapdetail.domain.model.MapPost
import com.moamap.app.feature.mapdetail.domain.model.MapPostComment
import com.moamap.app.feature.mapdetail.domain.model.MapPostPage
import com.moamap.app.feature.mapdetail.domain.model.MapPostSort
import com.moamap.app.feature.mapdetail.domain.model.NewMapPost
import com.moamap.app.feature.mapdetail.domain.repository.MapPostCommentRepository
import com.moamap.app.feature.mapdetail.domain.repository.MapPostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import java.io.IOException

private fun detailComment(id: Long, authorId: Long, content: String = "댓글$id") =
    MapPostComment(id, authorId, authorName = null, authorImageUrl = null, content = content, createdAtMillis = null)

private class FakeCommentRepository(
    var comments: List<MapPostComment> = listOf(detailComment(1, 2), detailComment(2, 1)),
) : MapPostCommentRepository {

    val loads = mutableListOf<Pair<Long, Long>>()
    val sent = mutableListOf<String>()
    var loadFailure: Exception? = null
    var sendFailure: Exception? = null
    var loadDelayMillis = 0L

    override suspend fun getComments(mapId: Long, postId: Long): List<MapPostComment> {
        loads += mapId to postId
        delay(loadDelayMillis)
        loadFailure?.let { throw it }
        return comments
    }

    override suspend fun createComment(mapId: Long, postId: Long, content: String): MapPostComment {
        sendFailure?.let { throw it }
        sent += content
        return detailComment(id = 100L + sent.size, authorId = 1, content = content)
    }
}

/** 게시물 하나를 읽는 쪽만 흉내 낸다. 알림에서 들어와 바로 열 때만 쓴다. */
private class FakePostRepository(var post: MapPost? = null) : MapPostRepository {
    val loads = mutableListOf<Pair<Long, Long>>()

    override suspend fun getPost(mapId: Long, postId: Long): MapPost {
        loads += mapId to postId
        return post ?: throw IOException("지워진 게시물")
    }

    override suspend fun getPosts(mapId: Long, page: Int, sort: MapPostSort): MapPostPage = TODO("상세는 목록을 읽지 않는다")

    override suspend fun uploadPhotos(mapId: Long, photos: List<Uri>): List<String> = TODO("상세는 사진을 올리지 않는다")

    override suspend fun createPost(mapId: Long, post: NewMapPost) = TODO("상세는 게시물을 쓰지 않는다")
}

@OptIn(ExperimentalCoroutinesApi::class)
class MapPostDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val post = MapPost(
        id = 5,
        authorId = 2,
        content = "성수 카페",
        imageUrls = emptyList(),
        placeNames = emptyList(),
        createdAtMillis = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: MapPostCommentRepository,
        myId: Long? = 1L,
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to 7L)),
        postRepository: MapPostRepository = FakePostRepository(),
    ) = MapPostDetailViewModel(
        savedStateHandle = savedStateHandle,
        repository = repository,
        currentUserStore = FakeCurrentUserStore(myId),
        postRepository = postRepository,
    )

    @Test
    fun `알림으로 들어오면 그 게시물을 읽어 바로 열고 경로에서 지운다`() = runTest {
        val handle = SavedStateHandle(
            mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to 7L, MoaMapRoute.MapDetail.ARG_POST_ID to 5L),
        )
        val postRepository = FakePostRepository(post)
        val viewModel = viewModel(FakeCommentRepository(), savedStateHandle = handle, postRepository = postRepository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(7L to 5L), postRepository.loads)
        assertEquals(post, viewModel.uiState.value.post)
        // 닫은 뒤 화면을 돌려 다시 만들어져도 또 열리지 않는다.
        assertEquals(MoaMapRoute.MapDetail.NO_POST_ID, handle.get<Long>(MoaMapRoute.MapDetail.ARG_POST_ID))
    }

    @Test
    fun `알림의 게시물이 지워졌으면 열지 않는다`() = runTest {
        val handle = SavedStateHandle(
            mapOf(MoaMapRoute.MapDetail.ARG_MAP_ID to 7L, MoaMapRoute.MapDetail.ARG_POST_ID to 5L),
        )
        val viewModel = viewModel(FakeCommentRepository(), savedStateHandle = handle)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.post)
    }

    private fun opened(repository: FakeCommentRepository, myId: Long? = 1L) =
        viewModel(repository, myId).apply {
            open(post)
            dispatcher.scheduler.advanceUntilIdle()
        }

    @Test
    fun `열기 전에는 닫혀 있고 아무것도 읽지 않는다`() = runTest {
        val repository = FakeCommentRepository()

        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.post)
        assertTrue(repository.loads.isEmpty())
    }

    @Test
    fun `열면 그 게시물의 댓글과 내 id 를 읽는다`() = runTest {
        val repository = FakeCommentRepository()

        val viewModel = opened(repository, myId = 1L)

        val state = viewModel.uiState.value
        assertEquals(post, state.post)
        assertEquals(listOf(7L to 5L), repository.loads)
        assertEquals(listOf(1L, 2L), state.comments.map { it.id })
        assertEquals(1L, state.myId)
        assertFalse(state.loading)
        assertTrue(state.commentsReady)
    }

    @Test
    fun `댓글을 못 읽으면 안내하고 다시 시도하면 다시 읽는다`() = runTest {
        val repository = FakeCommentRepository().apply { loadFailure = IOException("boom") }
        val viewModel = opened(repository)

        assertEquals(COMMENT_LOAD_FAILED_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.commentsReady)

        repository.loadFailure = null
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.comments.size)
    }

    @Test
    fun `댓글을 남기면 다시 읽지 않고 맨 아래에 붙인다`() = runTest {
        val repository = FakeCommentRepository()
        val viewModel = opened(repository)

        viewModel.send("  좋아요  ")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("좋아요"), repository.sent)
        assertEquals("좋아요", state.comments.last().content)
        assertEquals(1, state.sentCount)
        assertFalse(state.sending)
        // 처음 열 때 한 번뿐이다.
        assertEquals(1, repository.loads.size)
    }

    @Test
    fun `빈 댓글은 보내지 않는다`() = runTest {
        val repository = FakeCommentRepository()
        val viewModel = opened(repository)

        viewModel.send("   ")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.sent.isEmpty())
        assertEquals(0, viewModel.uiState.value.sentCount)
    }

    @Test
    fun `500자를 넘는 댓글은 잘라 보낸다`() = runTest {
        val repository = FakeCommentRepository()
        val viewModel = opened(repository)

        viewModel.send("가".repeat(COMMENT_MAX_LENGTH + 10))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(COMMENT_MAX_LENGTH, repository.sent.single().length)
    }

    /** 늦게 온 목록이 먼저 남긴 댓글을 덮어 사라지게 하지 않는다. */
    @Test
    fun `댓글을 다 읽기 전에는 보내지 않는다`() = runTest {
        val repository = FakeCommentRepository().apply { loadDelayMillis = 1_000 }
        val viewModel = viewModel(repository).apply { open(post) }

        viewModel.send("좋아요")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.sent.isEmpty())
    }

    @Test
    fun `댓글을 못 남기면 목록은 두고 알린다`() = runTest {
        val repository = FakeCommentRepository().apply { sendFailure = IOException("boom") }
        val viewModel = opened(repository)

        viewModel.send("좋아요")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(COMMENT_SEND_FAILED_MESSAGE, state.sendErrorMessage)
        assertEquals(2, state.comments.size)
        assertEquals(0, state.sentCount)
        assertFalse(state.sending)
    }

    @Test
    fun `닫으면 비우고 다시 열면 새로 읽는다`() = runTest {
        val repository = FakeCommentRepository()
        val viewModel = opened(repository)

        viewModel.close()
        assertNull(viewModel.uiState.value.post)

        viewModel.open(post.copy(id = 6))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(7L to 5L, 7L to 6L), repository.loads)
        assertEquals(6L, viewModel.uiState.value.post?.id)
    }
}
