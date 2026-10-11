package com.moamap.app.feature.mypage.data.repository

import androidx.core.net.toUri
import com.moamap.app.core.common.upload.PhotoUploader
import com.moamap.app.core.common.upload.uploadOne
import com.moamap.app.feature.mypage.data.remote.ProfileUploadUrlRequestDto
import com.moamap.app.feature.mypage.data.remote.UpdateMyPageRequestDto
import com.moamap.app.feature.mypage.data.remote.UserService
import com.moamap.app.feature.mypage.domain.model.MyProfile
import com.moamap.app.feature.mypage.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
// PhotoUploader 가 internal 이라 함께 internal 이다. MapRepositoryImpl 도 같다.
internal class UserRepositoryImpl @Inject constructor(
    private val userService: UserService,
    private val uploader: PhotoUploader,
) : UserRepository {

    override suspend fun getMyProfile(): MyProfile = userService.getMyPage().toMyProfile()

    override suspend fun uploadProfileImage(imageUri: String): String =
        uploader.uploadOne(imageUri.toUri()) { contentType, fileSize ->
            userService.createProfileUploadUrl(ProfileUploadUrlRequestDto(contentType = contentType, fileSize = fileSize))
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
