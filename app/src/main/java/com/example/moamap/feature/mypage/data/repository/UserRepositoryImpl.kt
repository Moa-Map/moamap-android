package com.example.moamap.feature.mypage.data.repository

import android.net.Uri
import com.example.moamap.core.common.upload.PhotoUploader
import com.example.moamap.core.common.upload.validateImageUpload
import com.example.moamap.feature.mypage.data.remote.ProfileUploadUrlRequestDto
import com.example.moamap.feature.mypage.data.remote.UpdateMyPageRequestDto
import com.example.moamap.feature.mypage.data.remote.UserService
import com.example.moamap.feature.mypage.domain.model.MyProfile
import com.example.moamap.feature.mypage.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
// PhotoUploader 가 internal 이라 함께 internal 이다. MapRepositoryImpl 도 같다.
internal class UserRepositoryImpl @Inject constructor(
    private val userService: UserService,
    private val uploader: PhotoUploader,
) : UserRepository {

    override suspend fun getMyProfile(): MyProfile = userService.getMyPage().toMyProfile()

    /**
     * 살펴보기 → 검증 → 발급 → 업로드 순으로 간다.
     *
     * 내용은 살펴볼 때 읽지 않는다. 올리는 순간 URI 에서 곧바로 흘려보낸다.
     */
    override suspend fun uploadProfileImage(imageUri: String): String {
        val photo = uploader.inspect(Uri.parse(imageUri))
        validateImageUpload(contentType = photo.contentType, fileSize = photo.size)

        val issued = userService.createProfileUploadUrl(
            ProfileUploadUrlRequestDto(contentType = photo.contentType, fileSize = photo.size),
        )
        require(issued.uploadUrl.isNotBlank() && issued.fileUrl.isNotBlank()) {
            "프로필 이미지 업로드 주소가 비어 있습니다"
        }

        uploader.upload(uploadUrl = issued.uploadUrl, photo = photo)
        return issued.fileUrl
    }

    // 나머지 필드는 기본값 null 이라 직렬화에서 빠진다. 부분 수정이므로 서버가 건드리지 않는다.
    override suspend fun updateMyProfile(
        nickname: String,
        introduction: String,
        profileImageUrl: String?,
    ): MyProfile = userService.updateMyPage(
        UpdateMyPageRequestDto(
            nickname = nickname,
            introduction = introduction,
            profileImageUrl = profileImageUrl,
        ),
    ).toMyProfile()
}
