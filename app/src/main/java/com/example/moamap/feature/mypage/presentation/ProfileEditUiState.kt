package com.example.moamap.feature.mypage.presentation

import androidx.compose.runtime.Immutable

/** 서버의 `@Size(max = 30)` 과 같은 값. 서버 규칙이 바뀌면 여기도 같이 바꾼다. */
const val NICKNAME_MAX_LENGTH = 30

/** 화면에 들어올 때 한 번 하는 조회의 상태. */
sealed interface ProfileLoadState {
    data object Loading : ProfileLoadState

    /**
     * 보여주기만 하는 값들.
     *
     * 편집 중인 이름·자기소개는 [ProfileEditUiState] 가 따로 들고 있다. 여기에 같이 넣으면
     * 글자를 한 번 칠 때마다 조회 상태를 copy 해야 해서 두 관심사가 얽힌다.
     */
    data class Success(
        val email: String,
        val profileImageUrl: String?,
    ) : ProfileLoadState

    data class Error(val message: String) : ProfileLoadState
}

/**
 * 올려둔 사진. 저장이 실패해 다시 눌러도 같은 사진을 두 번 올리지 않으려고 들고 있다.
 *
 * [sourceUri] 를 함께 들고 있어야 사진을 바꿔 고른 뒤에도 옛 주소를 쓰는 일이 없다.
 */
@Immutable
data class UploadedProfileImage(val sourceUri: String, val fileUrl: String)

@Immutable
data class ProfileEditUiState(
    val load: ProfileLoadState = ProfileLoadState.Loading,
    val nickname: String = "",
    val introduction: String = "",
    /** 사용자가 고른 사진. 미리보기가 서버 URL 대신 이걸 그린다. 저장 전까지 올리지 않는다. */
    val pickedImageUri: String? = null,
    val uploadedImage: UploadedProfileImage? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null,
) {
    /**
     * 서버가 400 을 줄 것이 뻔한 입력은 보내지 않는다.
     *
     * 서버 규칙이 `\S.*` 라 앞에 공백이 있으면 거절당하므로, 길이는 잘라낸 뒤 기준으로 센다.
     */
    val canSave: Boolean
        get() = load is ProfileLoadState.Success &&
            !saving &&
            nickname.isNotBlank() &&
            nickname.trim().length <= NICKNAME_MAX_LENGTH
}
