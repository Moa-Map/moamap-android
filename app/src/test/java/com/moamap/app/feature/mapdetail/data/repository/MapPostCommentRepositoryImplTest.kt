package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.core.network.model.PageResponse
import com.moamap.app.feature.explore.data.remote.ReportRequestDto
import com.moamap.app.feature.mapdetail.domain.model.ReportReason
import com.moamap.app.feature.mapdetail.data.remote.MapPostCommentCreateRequestDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostCommentDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostCreateRequestDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostPhotoUploadUrlDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostPhotoUploadUrlRequestDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostService
import com.moamap.app.feature.mypage.data.remote.MyPageDto
import com.moamap.app.feature.mypage.data.remote.ProfileUploadUrlDto
import com.moamap.app.feature.mypage.data.remote.ProfileUploadUrlRequestDto
import com.moamap.app.feature.mypage.data.remote.UpdateMyPageRequestDto
import com.moamap.app.feature.mypage.data.remote.UserProfileDto
import com.moamap.app.feature.mypage.data.remote.UserService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

private data class CommentsCall(val mapId: Long, val postId: Long, val page: Int?, val size: Int?, val sort: String?)

private class CommentRecordingPostService(
    private val pages: List<List<MapPostCommentDto>>,
) : MapPostService {

    val calls = mutableListOf<CommentsCall>()
    val created = mutableListOf<MapPostCommentCreateRequestDto>()
    val deleted = mutableListOf<Triple<Long, Long, Long>>()
    val reported = mutableListOf<ReportRequestDto>()

    override suspend fun deleteComment(mapId: Long, postId: Long, commentId: Long) {
        deleted += Triple(mapId, postId, commentId)
    }

    override suspend fun reportComment(mapId: Long, postId: Long, commentId: Long, request: ReportRequestDto) {
        reported += request
    }

    override suspend fun getComments(
        mapId: Long,
        postId: Long,
        page: Int?,
        size: Int?,
        sort: String?,
    ): PageResponse<MapPostCommentDto> {
        calls += CommentsCall(mapId, postId, page, size, sort)
        val index = page ?: 0
        return PageResponse(content = pages.getOrElse(index) { emptyList() }, page = index, last = index >= pages.lastIndex)
    }

    override suspend fun createComment(
        mapId: Long,
        postId: Long,
        request: MapPostCommentCreateRequestDto,
    ): MapPostCommentDto {
        created += request
        return MapPostCommentDto(id = 99, mapPostId = postId, userId = 1, content = request.content)
    }

    override suspend fun getPost(mapId: Long, postId: Long): MapPostDto = TODO("사용하지 않음")

    override suspend fun getPosts(mapId: Long, page: Int?, size: Int?, sort: String?): PageResponse<MapPostDto> =
        TODO("사용하지 않음")

    override suspend fun createPost(mapId: Long, request: MapPostCreateRequestDto): MapPostDto = TODO("사용하지 않음")

    override suspend fun createPhotoUploadUrl(
        mapId: Long,
        request: MapPostPhotoUploadUrlRequestDto,
    ): MapPostPhotoUploadUrlDto = TODO("사용하지 않음")
}

private class CommentProfileUserService(
    private val profiles: List<UserProfileDto> = emptyList(),
    private val failure: Exception? = null,
) : UserService {

    val asked = mutableListOf<List<Long>>()

    override suspend fun getProfiles(ids: List<Long>): List<UserProfileDto> {
        asked += ids
        failure?.let { throw it }
        return profiles.filter { profile -> profile.id in ids }
    }

    override suspend fun getMyPage(): MyPageDto = TODO("사용하지 않음")
    override suspend fun updateMyPage(request: UpdateMyPageRequestDto): MyPageDto = TODO("사용하지 않음")
    override suspend fun createProfileUploadUrl(request: ProfileUploadUrlRequestDto): ProfileUploadUrlDto =
        TODO("사용하지 않음")
}

class MapPostCommentRepositoryImplTest {

    private fun comment(id: Long, userId: Long, content: String = "댓글$id") =
        MapPostCommentDto(id = id, mapPostId = 5, userId = userId, content = content)

    @Test
    fun `오래된 것부터 끝 페이지까지 이어 받는다`() = runTest {
        val service = CommentRecordingPostService(pages = listOf(listOf(comment(1, 2)), listOf(comment(2, 3))))
        val repository = MapPostCommentRepositoryImpl(service, CommentProfileUserService())

        val comments = repository.getComments(mapId = 10, postId = 5)

        assertEquals(listOf(1L, 2L), comments.map { it.id })
        assertEquals(listOf(0, 1), service.calls.map { it.page })
        assertEquals("createdAt,asc", service.calls.first().sort)
        assertEquals(10L to 5L, service.calls.first().let { it.mapId to it.postId })
    }

    @Test
    fun `작성자 이름과 사진을 붙이고 같은 사람은 한 번만 묻는다`() = runTest {
        val service = CommentRecordingPostService(pages = listOf(listOf(comment(1, 2), comment(2, 2), comment(3, 3))))
        val users = CommentProfileUserService(
            profiles = listOf(
                UserProfileDto(id = 2, nickname = "이서연", profileImageUrl = " https://cdn/2.jpg "),
                UserProfileDto(id = 3, nickname = "  ", profileImageUrl = null),
            ),
        )
        val repository = MapPostCommentRepositoryImpl(service, users)

        val comments = repository.getComments(mapId = 10, postId = 5)

        assertEquals(listOf(listOf(2L, 3L)), users.asked)
        assertEquals("이서연", comments[0].authorName)
        assertEquals("https://cdn/2.jpg", comments[0].authorImageUrl)
        // 빈 이름은 없는 것으로 본다. 화면이 대신 채운다.
        assertNull(comments[2].authorName)
    }

    @Test
    fun `프로필을 못 받아도 댓글은 보여준다`() = runTest {
        val service = CommentRecordingPostService(pages = listOf(listOf(comment(1, 2))))
        val repository = MapPostCommentRepositoryImpl(service, CommentProfileUserService(failure = IOException("boom")))

        val comments = repository.getComments(mapId = 10, postId = 5)

        assertEquals(listOf("댓글1"), comments.map { it.content })
        assertNull(comments.single().authorName)
    }

    @Test
    fun `댓글을 남기면 받은 댓글을 돌려준다`() = runTest {
        val service = CommentRecordingPostService(pages = emptyList())
        val repository = MapPostCommentRepositoryImpl(service, CommentProfileUserService())

        val created = repository.createComment(mapId = 10, postId = 5, content = "좋아요")

        assertEquals(listOf(MapPostCommentCreateRequestDto("좋아요")), service.created)
        assertEquals(99L, created.id)
        assertEquals("좋아요", created.content)
    }

    /** 서버 응답에는 작성자 이름·사진이 없다. 방금 단 댓글에도 내 사진이 보이게 내 프로필을 받아 채운다. */
    @Test
    fun `댓글을 남기면 내 프로필을 받아 이름과 사진을 채운다`() = runTest {
        val service = CommentRecordingPostService(pages = emptyList())
        val users = CommentProfileUserService(
            profiles = listOf(UserProfileDto(id = 1, nickname = "모아", profileImageUrl = "https://cdn/me.jpg")),
        )
        val repository = MapPostCommentRepositoryImpl(service, users)

        val created = repository.createComment(mapId = 10, postId = 5, content = "좋아요")

        assertEquals(listOf(listOf(1L)), users.asked)
        assertEquals("모아", created.authorName)
        assertEquals("https://cdn/me.jpg", created.authorImageUrl)
    }

    @Test
    fun `내 프로필을 못 받아도 남긴 댓글은 돌려준다`() = runTest {
        val service = CommentRecordingPostService(pages = emptyList())
        val repository = MapPostCommentRepositoryImpl(service, CommentProfileUserService(failure = IOException("boom")))

        val created = repository.createComment(mapId = 10, postId = 5, content = "좋아요")

        assertEquals(99L, created.id)
        assertNull(created.authorImageUrl)
    }

    @Test
    fun `댓글을 지우고 신고한다 - 신고 내용은 공백을 지우고 비면 싣지 않는다`() = runTest {
        val service = CommentRecordingPostService(pages = emptyList())
        val repository = MapPostCommentRepositoryImpl(service, CommentProfileUserService())

        repository.deleteComment(mapId = 7L, postId = 5L, commentId = 3L)
        repository.reportComment(mapId = 7L, postId = 5L, commentId = 3L, reason = ReportReason.Other, detail = " 광고 ")
        repository.reportComment(mapId = 7L, postId = 5L, commentId = 3L, reason = ReportReason.Spam, detail = "  ")

        assertEquals(listOf(Triple(7L, 5L, 3L)), service.deleted)
        assertEquals(
            listOf(ReportRequestDto(reason = "OTHER", detail = "광고"), ReportRequestDto(reason = "SPAM")),
            service.reported,
        )
    }
}
