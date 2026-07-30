package com.example.moamap.feature.mapdetail.data.repository

import android.util.Log
import com.example.moamap.feature.explore.data.remote.PlaceReviewCreateRequestDto
import com.example.moamap.feature.explore.data.remote.ReviewService
import com.example.moamap.feature.mapdetail.domain.model.PlaceReview
import com.example.moamap.feature.mapdetail.domain.repository.PlaceReviewRepository
import com.example.moamap.feature.mypage.data.remote.UserService
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PlaceReviewRepository"

/** 한 번에 받아 오는 후기 수. 서버 상한과 같은 값이라 왕복이 가장 적다. */
private const val REVIEW_PAGE_SIZE = 100

/** 최신 글이 먼저 오게 한다. 정렬을 안 주면 서버가 저장 순서(오래된 것부터)로 내려준다. */
private const val NEWEST_FIRST = "createdAt,desc"

/** 프로필 벌크 조회가 한 번에 받는 식별자 수. 서버 상한이다. */
private const val PROFILE_CHUNK_SIZE = 100

@Singleton
class PlaceReviewRepositoryImpl @Inject constructor(
    private val reviewService: ReviewService,
    private val userService: UserService,
) : PlaceReviewRepository {

    override suspend fun getReviews(placeId: Long): List<PlaceReview> {
        val dtos = collectAllPages { page ->
            reviewService.getReviews(
                placeId = placeId,
                page = page,
                size = REVIEW_PAGE_SIZE,
                sort = NEWEST_FIRST,
            )
        }
        if (dtos.isEmpty()) return emptyList()

        val nicknames = fetchNicknames(dtos.map { dto -> dto.userId })
        return dtos.map { dto -> dto.toPlaceReview(authorName = nicknames[dto.userId]) }
    }

    override suspend fun createReview(placeId: Long, rating: Int, content: String) {
        reviewService.createReview(
            placeId = placeId,
            request = PlaceReviewCreateRequestDto(
                rating = rating,
                // 별점만 남기는 것도 서버가 받아 준다. 빈 문자열 대신 자리를 비워 보낸다.
                content = content.takeIf { text -> text.isNotBlank() },
            ),
        )
    }

    /**
     * 작성자 닉네임을 식별자로 찾아 둔다.
     *
     * 곁들이는 정보라 실패를 삼킨다. 이름 한 줄 때문에 후기 목록을 통째로 못 여는 게 더 나쁘다.
     * 같은 사람이 여러 건을 남길 수 있어 중복을 지우고 묻는다.
     */
    private suspend fun fetchNicknames(authorIds: List<Long>): Map<Long, String> {
        val ids = authorIds.filter { id -> id > 0 }.distinct()
        if (ids.isEmpty()) return emptyMap()

        return try {
            ids.chunked(PROFILE_CHUNK_SIZE)
                .flatMap { chunk -> userService.getProfiles(chunk) }
                .mapNotNull { profile ->
                    val nickname = profile.nickname?.takeIf { name -> name.isNotBlank() }
                    nickname?.let { name -> profile.id to name }
                }
                .toMap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 사용자 식별자는 남기지 않는다. 로그가 수집·보관되는 경로를 타기 때문이다.
            Log.w(TAG, "후기 작성자 프로필 조회 실패", e)
            emptyMap()
        }
    }
}
