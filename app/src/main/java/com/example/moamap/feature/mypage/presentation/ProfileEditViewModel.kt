package com.example.moamap.feature.mypage.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun save() {
        val current = _uiState.value
        if (!current.canSave) return
        _uiState.update { it.copy(saving = true) }

        viewModelScope.launch {
            try {
                userRepository.updateMyProfile(
                    nickname = current.nickname.trim(),
                    introduction = current.introduction,
                )
                _uiState.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                // 저장이 안 됐는데 화면을 닫으면 고친 내용이 사라진 걸 사용자가 모른다.
                Log.e(TAG, "프로필 저장 실패", throwable)
                _uiState.update { it.copy(saving = false, errorMessage = SAVE_ERROR) }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
