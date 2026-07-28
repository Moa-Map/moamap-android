package com.example.moamap.feature.collection.presentation.createmap

import androidx.lifecycle.SavedStateHandle
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
 * 아직 `POST /api/v1/maps` 를 붙이지 않아 입력 상태만 다룬다. 입력 도중 프로세스가 죽어도
 * 돌아왔을 때 이어서 쓸 수 있도록 모든 값을 [SavedStateHandle] 에 함께 남긴다.
 */
@HiltViewModel
internal class CreateMapViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(savedStateHandle.toCreateMapUiState())
    val uiState: StateFlow<CreateMapUiState> = _uiState.asStateFlow()

    fun selectImage(uri: String) {
        updateState { state -> state.copy(imageUri = uri) }
    }

    fun updateName(name: String) {
        updateState { state -> state.copy(name = name.take(NAME_MAX_LENGTH)) }
    }

    fun updateDescription(description: String) {
        updateState { state ->
            state.copy(description = description.take(DESCRIPTION_MAX_LENGTH))
        }
    }

    fun selectVisibility(visibility: MapVisibility) {
        updateState { state -> state.copy(visibility = visibility) }
    }

    /**
     * 태그 입력값 변경.
     *
     * 구분자가 섞여 들어오면 그 앞까지는 태그로 확정하고 나머지만 입력값으로 남긴다.
     * 붙여넣기로 여러 개가 한 번에 들어오는 경우도 같은 규칙으로 처리된다.
     */
    fun updateTagInput(input: String) {
        if (input.none { char -> char in TAG_SEPARATORS }) {
            updateState { state -> state.copy(tagInput = input.take(TAG_MAX_LENGTH)) }
            return
        }

        val tokens = input.split(*TAG_SEPARATORS)
        // 마지막 토큰은 구분자 뒤에 남은 값이라 아직 확정된 것이 아니다.
        val pending = tokens.last()

        updateState { state ->
            state.copy(
                tags = state.tags.plusTags(tokens.dropLast(1)),
                tagInput = pending.take(TAG_MAX_LENGTH),
            )
        }
    }

    /** 엔터로 확정할 때 쓴다. */
    fun commitTag() {
        updateState { state ->
            state.copy(
                tags = state.tags.plusTags(listOf(state.tagInput)),
                tagInput = "",
            )
        }
    }

    fun removeTag(tag: String) {
        updateState { state -> state.copy(tags = state.tags - tag) }
    }

    /** 상태 변경과 저장을 한 자리에 묶어, 저장을 빠뜨린 경로가 생기지 않게 한다. */
    private fun updateState(transform: (CreateMapUiState) -> CreateMapUiState) {
        val next = transform(_uiState.value)
        _uiState.value = next
        savedStateHandle.save(next)
    }
}

private const val KEY_IMAGE_URI = "createMap.imageUri"
private const val KEY_NAME = "createMap.name"
private const val KEY_DESCRIPTION = "createMap.description"
private const val KEY_VISIBILITY = "createMap.visibility"
private const val KEY_TAGS = "createMap.tags"
private const val KEY_TAG_INPUT = "createMap.tagInput"

/**
 * 저장된 값에서 상태를 되살린다.
 *
 * [CreateMapUiState] 를 통째로 넣지 않고 항목별로 쪼개 둔다 - Bundle 에 담으려면 Parcelable
 * 이어야 하는데, 그러자고 화면 상태에 직렬화 형식을 끌어들일 이유가 없다.
 */
private fun SavedStateHandle.toCreateMapUiState() = CreateMapUiState(
    imageUri = get<String>(KEY_IMAGE_URI),
    name = get<String>(KEY_NAME).orEmpty(),
    description = get<String>(KEY_DESCRIPTION).orEmpty(),
    // 저장한 뒤 enum 이 바뀌었을 수 있으니 모르는 값은 고르지 않은 것으로 본다.
    visibility = get<String>(KEY_VISIBILITY)?.let { saved ->
        MapVisibility.entries.firstOrNull { it.name == saved }
    },
    tags = get<ArrayList<String>>(KEY_TAGS).orEmpty(),
    tagInput = get<String>(KEY_TAG_INPUT).orEmpty(),
)

private fun SavedStateHandle.save(state: CreateMapUiState) {
    this[KEY_IMAGE_URI] = state.imageUri
    this[KEY_NAME] = state.name
    this[KEY_DESCRIPTION] = state.description
    this[KEY_VISIBILITY] = state.visibility?.name
    // Bundle 이 담을 수 있는 형태여야 해서 ArrayList 로 넘긴다.
    this[KEY_TAGS] = ArrayList(state.tags)
    this[KEY_TAG_INPUT] = state.tagInput
}

/** 빈 값과 이미 담긴 태그는 걸러낸다. 한 번에 들어온 값들 사이의 중복도 마찬가지다. */
private fun List<String>.plusTags(candidates: List<String>): List<String> {
    val added = candidates
        .map { candidate -> candidate.trim().take(TAG_MAX_LENGTH) }
        .filter { candidate -> candidate.isNotEmpty() }

    return (this + added).distinct()
}
