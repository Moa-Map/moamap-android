package com.moamap.app.feature.mapdetail.data.repository

import android.net.Uri
import com.moamap.app.core.common.upload.PhotoSpec
import com.moamap.app.core.common.upload.PhotoUploader
import com.moamap.app.core.network.model.PageResponse
import com.moamap.app.feature.explore.data.remote.PlaceReviewCreateRequestDto
import com.moamap.app.feature.explore.data.remote.PlaceReviewDto
import com.moamap.app.feature.explore.data.remote.PlaceReviewPhotoUploadUrlDto
import com.moamap.app.feature.explore.data.remote.PlaceReviewPhotoUploadUrlRequestDto
import com.moamap.app.feature.explore.data.remote.PlaceReviewUpdateRequestDto
import com.moamap.app.feature.explore.data.remote.ReviewService
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

/** 작성 요청만 받아 둔다. */
private class RecordingReviewService : ReviewService {

    val created = mutableListOf<PlaceReviewCreateRequestDto>()

    override suspend fun createReview(
        placeId: Long,
        request: PlaceReviewCreateRequestDto,
    ): PlaceReviewDto {
        created += request
        return PlaceReviewDto()
    }

    override suspend fun getReviews(placeId: Long, page: Int?, size: Int?, sort: String?):
        PageResponse<PlaceReviewDto> = TODO("사용하지 않음")

    override suspend fun createPhotoUploadUrl(
        placeId: Long,
        request: PlaceReviewPhotoUploadUrlRequestDto,
    ): PlaceReviewPhotoUploadUrlDto = TODO("사진 없는 경로만 검증한다")

    override suspend fun updateReview(
        placeId: Long,
        reviewId: Long,
        request: PlaceReviewUpdateRequestDto,
    ): PlaceReviewDto = TODO("사용하지 않음")

    override suspend fun deleteReview(placeId: Long, reviewId: Long) = TODO("사용하지 않음")
}

private class UnusedReviewUserService : UserService {
    override suspend fun getMyPage(): MyPageDto = TODO("사용하지 않음")
    override suspend fun updateMyPage(request: UpdateMyPageRequestDto): MyPageDto = TODO("사용하지 않음")
    override suspend fun getProfiles(ids: List<Long>): List<UserProfileDto> = TODO("사용하지 않음")
    override suspend fun createProfileUploadUrl(request: ProfileUploadUrlRequestDto):
        ProfileUploadUrlDto = TODO("사용하지 않음")
}

/** `Uri` 를 JVM 테스트에서 만들 수 없어 사진 없는 경로만 검증한다. */
private class NoReviewPhotoUploader : PhotoUploader {
    override suspend fun inspect(uri: Uri): PhotoSpec = TODO("사진 없는 경로만 검증한다")
    override suspend fun upload(uploadUrl: String, photo: PhotoSpec) = TODO("사진 없는 경로만 검증한다")
}

class PlaceReviewRepositoryImplTest {

    private val service = RecordingReviewService()
    private val repository = PlaceReviewRepositoryImpl(
        reviewService = service,
        userService = UnusedReviewUserService(),
        uploader = NoReviewPhotoUploader(),
    )

    @Test
    fun `화면에 별점이 없어도 서버가 요구하는 별점을 고정값으로 싣는다`() = runTest {
        repository.createReview(placeId = 7L, content = "좋았어요", photo = null)

        val request = service.created.single()
        assertEquals(5, request.rating)
        assertEquals("좋았어요", request.content)
        assertNull(request.imageUrls)
    }

    @Test
    fun `빈 글은 자리를 비워 보낸다`() = runTest {
        repository.createReview(placeId = 7L, content = "  ", photo = null)

        assertNull(service.created.single().content)
    }
}
