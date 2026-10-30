package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.explore.data.remote.ReportRequestDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostCommentCreateRequestDto
import com.moamap.app.feature.mapdetail.data.remote.MapPostService
import com.moamap.app.feature.mapdetail.domain.model.MapPostComment
import com.moamap.app.feature.mapdetail.domain.model.ReportReason
import com.moamap.app.feature.mapdetail.domain.repository.MapPostCommentRepository
import com.moamap.app.feature.mypage.data.remote.UserService
import javax.inject.Inject
import javax.inject.Singleton

/** 한 번에 받아 오는 댓글 수. 장소 댓글과 같은 값이다. */
private const val COMMENT_PAGE_SIZE = 100

/** 대화처럼 오래된 것이 위, 새 것이 아래다. 서버 기본값과 같지만 바뀌어도 화면과 어긋나지 않게 보낸다. */
private const val OLDEST_FIRST = "createdAt,asc"

@Singleton
internal class MapPostCommentRepositoryImpl @Inject constructor(
    private val mapPostService: MapPostService,
    private val userService: UserService,
) : MapPostCommentRepository {

    override suspend fun getComments(mapId: Long, postId: Long): List<MapPostComment> {
        val dtos = collectAllPages { page ->
            mapPostService.getComments(
                mapId = mapId,
                postId = postId,
                page = page,
                size = COMMENT_PAGE_SIZE,
                sort = OLDEST_FIRST,
            )
        }
        val profiles = userService.fetchAuthorProfiles(dtos.map { dto -> dto.userId })
        return dtos.map { dto ->
            val profile = profiles[dto.userId]
            dto.toMapPostComment(authorName = profile?.nickname, authorImageUrl = profile?.profileImageUrl)
        }
    }

    /**
     * 서버는 남긴 댓글에 작성자 이름·사진을 주지 않는다. 내 프로필을 한 번 받아 채워, 방금 단 댓글에도 「나」 옆
     * 내 사진이 바로 보이게 한다. 프로필을 못 받으면 이름·사진 없이 돌려준다 - 댓글은 이미 남았다.
     */
    override suspend fun createComment(mapId: Long, postId: Long, content: String): MapPostComment {
        val dto = mapPostService.createComment(
            mapId = mapId,
            postId = postId,
            request = MapPostCommentCreateRequestDto(content = content),
        )
        val profile = userService.fetchAuthorProfiles(listOf(dto.userId))[dto.userId]
        return dto.toMapPostComment(authorName = profile?.nickname, authorImageUrl = profile?.profileImageUrl)
    }

    override suspend fun deleteComment(mapId: Long, postId: Long, commentId: Long) =
        mapPostService.deleteComment(mapId = mapId, postId = postId, commentId = commentId)

    override suspend fun reportComment(
        mapId: Long,
        postId: Long,
        commentId: Long,
        reason: ReportReason,
        detail: String,
    ) = mapPostService.reportComment(
        mapId = mapId,
        postId = postId,
        commentId = commentId,
        request = ReportRequestDto(reason = reason.serverValue, detail = detail.trim().ifEmpty { null }),
    )
}
