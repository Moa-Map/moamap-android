package com.moamap.app.feature.mapdetail.domain.repository

import com.moamap.app.feature.mapdetail.domain.model.MapPostComment
import com.moamap.app.feature.mapdetail.domain.model.ReportReason

/** 로그 탭 게시물의 댓글. 게시물 상세를 열 때만 쓴다. */
interface MapPostCommentRepository {

    /** 오래된 것부터 전부. 작성자 이름·사진을 붙여 준다. */
    suspend fun getComments(mapId: Long, postId: Long): List<MapPostComment>

    /** 남긴 댓글을 돌려준다. 내 댓글이라 작성자 이름·사진은 채우지 않는다. */
    suspend fun createComment(mapId: Long, postId: Long, content: String): MapPostComment

    suspend fun deleteComment(mapId: Long, postId: Long, commentId: Long)

    /** 남의 댓글을 신고한다. [detail] 은 앞뒤 공백을 지우고 비면 싣지 않는다. */
    suspend fun reportComment(mapId: Long, postId: Long, commentId: Long, reason: ReportReason, detail: String)
}
