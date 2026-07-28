package com.example.moamap.feature.collection.presentation.createmap

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** 태그를 확정하는 구분자. 플레이스홀더가 안내하는 "스페이스 또는 엔터" 와 같다. */
private val TAG_SEPARATORS = charArrayOf(' ', '\n')

/**
 * 새 지도 만들기 화면 ViewModel.
 *
 * 아직 `POST /api/v1/maps` 를 붙이지 않아 입력 상태만 다룬다.
 */
@HiltViewModel
internal class CreateMapViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CreateMapUiState())
    val uiState: StateFlow<CreateMapUiState> = _uiState.asStateFlow()

    fun selectImage(uri: String) {
        _uiState.update { state -> state.copy(imageUri = uri) }
    }

    fun updateName(name: String) {
        _uiState.update { state -> state.copy(name = name.take(NAME_MAX_LENGTH)) }
    }

    fun updateDescription(description: String) {
        _uiState.update { state ->
            state.copy(description = description.take(DESCRIPTION_MAX_LENGTH))
        }
    }

    fun selectVisibility(visibility: MapVisibility) {
        _uiState.update { state -> state.copy(visibility = visibility) }
    }

    /**
     * 태그 입력값 변경.
     *
     * 구분자가 섞여 들어오면 그 앞까지는 태그로 확정하고 나머지만 입력값으로 남긴다.
     * 붙여넣기로 여러 개가 한 번에 들어오는 경우도 같은 규칙으로 처리된다.
     */
    fun updateTagInput(input: String) {
        if (input.none { char -> char in TAG_SEPARATORS }) {
            _uiState.update { state -> state.copy(tagInput = input.take(TAG_MAX_LENGTH)) }
            return
        }

        val tokens = input.split(*TAG_SEPARATORS)
        // 마지막 토큰은 구분자 뒤에 남은 값이라 아직 확정된 것이 아니다.
        val pending = tokens.last()

        _uiState.update { state ->
            state.copy(
                tags = state.tags.plusTags(tokens.dropLast(1)),
                tagInput = pending.take(TAG_MAX_LENGTH),
            )
        }
    }

    /** 엔터로 확정할 때 쓴다. */
    fun commitTag() {
        _uiState.update { state ->
            state.copy(
                tags = state.tags.plusTags(listOf(state.tagInput)),
                tagInput = "",
            )
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { state -> state.copy(tags = state.tags - tag) }
    }
}

/** 빈 값과 이미 담긴 태그는 걸러낸다. 한 번에 들어온 값들 사이의 중복도 마찬가지다. */
private fun List<String>.plusTags(candidates: List<String>): List<String> {
    val added = candidates
        .map { candidate -> candidate.trim().take(TAG_MAX_LENGTH) }
        .filter { candidate -> candidate.isNotEmpty() }

    return (this + added).distinct()
}
