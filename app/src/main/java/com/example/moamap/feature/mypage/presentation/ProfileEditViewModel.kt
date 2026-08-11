package com.example.moamap.feature.mypage.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.core.common.upload.ImageUploadException
import com.example.moamap.feature.mypage.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ProfileEditViewModel"
private const val LOAD_ERROR = "프로필을 불러오지 못했어요. 잠시 후 다시 시도해주세요."
private const val SAVE_ERROR = "저장하지 못했어요. 잠시 후 다시 시도해주세요."

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(load = ProfileLoadState.Loading) }

        viewModelScope.launch {
            try {
                val profile = userRepository.getMyProfile()
                _uiState.update { state ->
                    state.copy(
                        load = ProfileLoadState.Success(
                            email = profile.email,
                            profileImageUrl = profile.profileImageUrl,
                        ),
                        nickname = profile.nickname,
                        introduction = profile.introduction,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.e(TAG, "프로필 조회 실패", throwable)
                _uiState.update { it.copy(load = ProfileLoadState.Error(LOAD_ERROR)) }
            }
        }
    }

    fun onNicknameChange(value: String) {
        _uiState.update { it.copy(nickname = value) }
    }

    fun onIntroductionChange(value: String) {
        _uiState.update { it.copy(introduction = value) }
    }

    /**
     * 다른 사진을 골랐으면 앞서 올려둔 주소는 쓸모가 없다.
     *
     * 저장 중에는 무시한다 — `save()` 는 호출 시점의 사진을 스냅샷으로 들고 이미 업로드를
     * 시작했으므로, 지금 바꿔봐야 서버에는 먼저 고른 사진이 저장된다. 그런데 바꾸는 걸 허용하면
     * 화면은 나중에 고른 사진을 보여주며 저장이 끝났다고 알리게 돼 실제로 저장된 사진과 어긋난다.
     */
    fun onImageSelected(uri: String) {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(pickedImageUri = uri, uploadedImage = null) }
    }

    fun save() {
        val current = _uiState.value
        if (!current.canSave) return
        _uiState.update { it.copy(saving = true) }

        viewModelScope.launch {
            try {
                val imageUrl = current.pickedImageUri?.let { uri -> resolveImageUrl(uri) }
                userRepository.updateMyProfile(
                    nickname = current.nickname.trim(),
                    introduction = current.introduction,
                    profileImageUrl = imageUrl,
                )
                _uiState.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                // 저장이 안 됐는데 화면을 닫으면 고친 내용이 사라진 걸 사용자가 모른다.
                Log.e(TAG, "프로필 저장 실패", throwable)
                _uiState.update {
                    it.copy(saving = false, errorMessage = throwable.toSaveMessage())
                }
            }
        }
    }

    /**
     * 같은 사진을 이미 올렸으면 그 주소를 그대로 쓴다.
     *
     * 올린 파일을 지우는 API 가 없어, 저장이 실패할 때마다 새로 올리면 지울 수 없는 사진이 쌓인다.
     */
    private suspend fun resolveImageUrl(uri: String): String =
        _uiState.value.uploadedImage?.takeIf { it.sourceUri == uri }?.fileUrl
            ?: userRepository.uploadProfileImage(uri).also { fileUrl ->
                _uiState.update { it.copy(uploadedImage = UploadedProfileImage(uri, fileUrl)) }
            }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * 업로드 실패는 저장 실패와 다르게 안내한다.
 *
 * 둘 다 "저장하지 못했어요" 로 뭉개면 사용자가 할 조치를 알 수 없다 - 사진을 바꿔야 하는 경우와
 * 그냥 다시 눌러야 하는 경우가 다르다.
 */
private fun Throwable.toSaveMessage(): String = when (this) {
    // 무엇이 문제인지는 예외가 이미 문구로 들고 있다.
    is ImageUploadException -> message ?: SAVE_ERROR
    else -> SAVE_ERROR
}
