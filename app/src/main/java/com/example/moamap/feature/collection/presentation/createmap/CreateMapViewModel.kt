package com.example.moamap.feature.collection.presentation.createmap

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.core.network.ConnectionException
import com.example.moamap.feature.collection.domain.model.MapVisibility
import com.example.moamap.feature.collection.domain.model.NewMap
import com.example.moamap.feature.collection.domain.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CreateMapViewModel"
private const val CREATE_FAILED_MESSAGE = "지도를 만들지 못했어요"
private const val NETWORK_ERROR_MESSAGE = "네트워크에 연결할 수 없어요"

/** 태그를 확정하는 구분자. 플레이스홀더가 안내하는 "스페이스 또는 엔터" 와 같다. */
private val TAG_SEPARATORS = charArrayOf(' ', '\n')

/**
 * 새 지도 만들기 화면 ViewModel.
 *
 * 입력 도중 프로세스가 죽어도 돌아왔을 때 이어서 쓸 수 있도록 모든 값을 [SavedStateHandle] 에
 * 함께 남긴다.
 */
@HiltViewModel
internal class CreateMapViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val mapRepository: MapRepository,
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

    /**
     * 지도를 만든다.
     *
     * 고른 사진은 함께 보내지 않는다 - 서버에 커버 이미지 업로드 창구가 없다.
     */
    fun submit() {
        if (!_uiState.value.canSubmit) return

        // 구분자 없이 입력만 해두고 바로 누른 태그도 확정한다. 그냥 두면 tagInput 에만 남아
        // 요청에서 조용히 빠진다. 화면의 칩과 보내는 값을 어긋나지 않게 상태부터 확정한다.
        commitTag()

        val current = _uiState.value
        val visibility = current.visibility ?: return
        val newMap = NewMap(
            name = current.name,
            description = current.description,
            visibility = visibility,
            tags = current.tags,
        )

        // 코루틴이 시작되기를 기다리지 않고 여기서 잠근다. 시작 시점에 잠그면 그 전에
        // 들어온 두 번째 탭이 가드를 그대로 통과해 지도가 두 개 만들어진다.
        updateState { state ->
            state.copy(submit = SubmitState.Submitting, errorMessage = null)
        }

        viewModelScope.launch {
            try {
                val created = mapRepository.createMap(newMap)
                // 프라이빗 지도는 초대 코드를 먼저 보여주고, 닫을 때 화면을 뺀다.
                val next = created.inviteCode
                    ?.let { code -> SubmitState.ShowingInviteCode(created.id, code) }
                    ?: SubmitState.Done(created.id)
                updateState { state -> state.copy(submit = next) }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.e(TAG, "지도 생성 실패", throwable)
                // 입력값은 그대로 둔다. 이름·설명·태그를 다시 치게 만들면 안 된다.
                updateState { state ->
                    state.copy(
                        submit = SubmitState.Idle,
                        errorMessage = throwable.toUserMessage(),
                    )
                }
            }
        }
    }

    /** 초대 코드를 다 본 뒤. 지도는 이미 만들어졌으므로 그대로 화면을 뺀다. */
    fun dismissInviteCode() {
        val showing = _uiState.value.submit as? SubmitState.ShowingInviteCode ?: return
        updateState { state -> state.copy(submit = SubmitState.Done(showing.mapId)) }
    }

    /** 안내를 보여준 뒤 호출한다. 같은 메시지가 다시 뜨지 않게 한다. */
    fun consumeError() {
        updateState { state -> state.copy(errorMessage = null) }
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
private const val KEY_CREATED_MAP_ID = "createMap.createdMapId"
private const val KEY_INVITE_CODE = "createMap.inviteCode"

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
    // 진행 중 상태는 복원하지 않는다. 요청은 프로세스와 함께 사라졌는데 "만드는 중" 으로
    // 되살아나면 버튼이 영영 잠긴다.
    //
    // 초대 코드는 되살린다. 지도는 이미 만들어졌고, 이 화면을 벗어나면 코드를 다시 볼
    // 방법이 없다.
    submit = restoreInviteCode() ?: SubmitState.Idle,
)

private fun SavedStateHandle.restoreInviteCode(): SubmitState.ShowingInviteCode? {
    val mapId = get<Long>(KEY_CREATED_MAP_ID) ?: return null
    val code = get<String>(KEY_INVITE_CODE)?.takeIf { it.isNotBlank() } ?: return null
    return SubmitState.ShowingInviteCode(mapId, code)
}

private fun SavedStateHandle.save(state: CreateMapUiState) {
    this[KEY_IMAGE_URI] = state.imageUri
    this[KEY_NAME] = state.name
    this[KEY_DESCRIPTION] = state.description
    this[KEY_VISIBILITY] = state.visibility?.name
    // Bundle 이 담을 수 있는 형태여야 해서 ArrayList 로 넘긴다.
    this[KEY_TAGS] = ArrayList(state.tags)
    this[KEY_TAG_INPUT] = state.tagInput

    val showing = state.submit as? SubmitState.ShowingInviteCode
    this[KEY_CREATED_MAP_ID] = showing?.mapId
    this[KEY_INVITE_CODE] = showing?.inviteCode
}

/** 빈 값과 이미 담긴 태그는 걸러낸다. 한 번에 들어온 값들 사이의 중복도 마찬가지다. */
private fun List<String>.plusTags(candidates: List<String>): List<String> {
    val added = candidates
        .map { candidate -> candidate.trim().take(TAG_MAX_LENGTH) }
        .filter { candidate -> candidate.isNotEmpty() }

    return (this + added).distinct()
}

private fun Throwable.toUserMessage(): String = when (this) {
    // 서버 메시지는 "[500] COMMON_005: ..." 처럼 사용자에게 보여줄 형태가 아니다.
    is ConnectionException -> NETWORK_ERROR_MESSAGE
    else -> CREATE_FAILED_MESSAGE
}
