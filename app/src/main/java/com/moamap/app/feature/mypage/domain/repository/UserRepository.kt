package com.moamap.app.feature.mypage.domain.repository

import com.moamap.app.feature.mypage.domain.model.MyProfile

interface UserRepository {

    /** 내 프로필. 프로필 편집 화면에 들어올 때 한 번 부른다. */
    suspend fun getMyProfile(): MyProfile

    /**
     * 고른 사진을 올리고 접근 주소를 돌려준다.
     *
     * 이 값을 [updateMyProfile] 에 담아야 실제로 반영된다. 올리기만 해서는 프로필이 바뀌지 않는다.
     *
     * @param imageUri 사용자가 고른 사진의 `content://` URI 문자열. 화면 상태가 들고 있는
     *  형태 그대로 받는다.
     * @return 수정 요청의 `profileImageUrl` 에 담을 주소
     * @throws com.moamap.app.core.common.upload.ImageUploadException
     *  형식이나 크기가 서버 허용 범위를 벗어날 때
     */
    suspend fun uploadProfileImage(imageUri: String): String

    /**
     * 이름과 자기소개를 고치고 갱신된 프로필을 돌려준다.
     * 이메일은 소셜 로그인이 정하는 값이라 화면에서 고칠 수 없다.
     *
     * @param profileImageUrl [uploadProfileImage] 가 돌려준 주소. null 이면 사진은 건드리지 않는다.
     */
    suspend fun updateMyProfile(
        nickname: String,
        introduction: String,
        profileImageUrl: String? = null,
    ): MyProfile
}
