package com.moamap.app.feature.mapdetail.data.repository

import android.net.Uri
import android.util.Log
import com.moamap.app.core.common.upload.MAX_REVIEW_PHOTO_FILE_SIZE
import com.moamap.app.core.common.upload.PhotoUploader
import com.moamap.app.core.common.upload.validateImageUpload
import com.moamap.app.feature.explore.data.remote.PlaceReviewCreateRequestDto
import com.moamap.app.feature.explore.data.remote.PlaceReviewPhotoUploadUrlRequestDto
import com.moamap.app.feature.explore.data.remote.ReviewService
import com.moamap.app.feature.mapdetail.domain.model.PlaceReview
import com.moamap.app.feature.mapdetail.domain.repository.PlaceReviewRepository
import com.moamap.app.feature.mypage.data.remote.UserService
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

/**
 * 후기에 싣는 별점.
 *
 * 화면에서 별점을 없앴지만 서버는 1~5 를 필수로 받는다. 평균 별점도 더는 보여주지 않아
 * 어떤 값이든 화면에 드러나지 않는다. 서버가 선택값으로 바꾸면 이 값과 함께 지운다.
 */
internal const val FIXED_REVIEW_RATING = 5

@Singleton
internal class PlaceReviewRepositoryImpl @Inject constructor(
    private val reviewService: ReviewService,
    private val userService: UserService,
    private val uploader: PhotoUploader,
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

    override suspend fun createReview(placeId: Long, content: String, photo: Uri?) {
        val imageUrl = photo?.let { uri -> uploadPhoto(placeId, uri) }
        reviewService.createReview(
            placeId = placeId,
            request = PlaceReviewCreateRequestDto(
                rating = FIXED_REVIEW_RATING,
                // 사진만 남기는 것도 서버가 받아 준다. 빈 문자열 대신 자리를 비워 보낸다.
                content = content.takeIf { text -> text.isNotBlank() },
                imageUrls = imageUrl?.let { url -> listOf(url) },
            ),
        )
    }

    /** 형식·크기를 먼저 거른 뒤 발급받아 올린다. 서버 400 을 받고 나서는 이유를 알려줄 수 없다. */
    private suspend fun uploadPhoto(placeId: Long, uri: Uri): String {
        val photo = uploader.inspect(uri)
        validateImageUpload(
            contentType = photo.contentType,
            fileSize = photo.size,
            maxFileSize = MAX_REVIEW_PHOTO_FILE_SIZE,
        )

        val issued = reviewService.createPhotoUploadUrl(
            placeId = placeId,
            request = PlaceReviewPhotoUploadUrlRequestDto(
                contentType = photo.contentType,
                fileSize = photo.size,
            ),
        )
        require(issued.uploadUrl.isNotBlank() && issued.fileUrl.isNotBlank()) {
            "사진 업로드 주소가 비어 있습니다"
        }
        uploader.upload(uploadUrl = issued.uploadUrl, photo = photo)
        return issued.fileUrl
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
