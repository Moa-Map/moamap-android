package com.moamap.app.feature.mapdetail.data.repository

import android.util.Log
import com.moamap.app.feature.mypage.data.remote.UserProfileDto
import com.moamap.app.feature.mypage.data.remote.UserService
import kotlinx.coroutines.CancellationException

private const val TAG = "AuthorProfiles"

/** 프로필 벌크 조회가 한 번에 받는 식별자 수. 서버 상한이다. */
private const val PROFILE_CHUNK_SIZE = 100

/**
 * 댓글 작성자 닉네임·프로필 사진을 식별자로 찾아 둔다. 장소 댓글과 게시물 댓글이 같이 쓴다. 빈 값은 매퍼가 거른다.
 *
 * 곁들이는 정보라 실패를 삼킨다. 이름 한 줄 때문에 댓글 목록을 통째로 못 여는 게 더 나쁘다.
 * 같은 사람이 여러 건을 남길 수 있어 중복을 지우고 묻는다.
 */
internal suspend fun UserService.fetchAuthorProfiles(authorIds: List<Long>): Map<Long, UserProfileDto> {
    val ids = authorIds.filter { id -> id > 0 }.distinct()
    if (ids.isEmpty()) return emptyMap()

    return try {
        ids.chunked(PROFILE_CHUNK_SIZE)
            .flatMap { chunk -> getProfiles(chunk) }
            .associateBy { profile -> profile.id }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 사용자 식별자는 남기지 않는다. 로그가 수집·보관되는 경로를 타기 때문이다.
        Log.w(TAG, "댓글 작성자 프로필 조회 실패", e)
        emptyMap()
    }
}
