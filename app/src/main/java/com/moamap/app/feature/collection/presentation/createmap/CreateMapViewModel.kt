package com.moamap.app.feature.collection.presentation.createmap

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.toUploadMessage
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.feature.collection.domain.model.EditedPlace
import com.moamap.app.feature.collection.domain.model.MapVisibility
import com.moamap.app.feature.collection.domain.model.NewMap
import com.moamap.app.feature.collection.domain.repository.MapRepository
import com.moamap.app.feature.collection.domain.repository.PlaceImportRepository
import com.moamap.app.feature.mapdetail.presentation.addplace.applyTagInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CreateMapViewModel"
private const val CREATE_FAILED_MESSAGE = "지도를 만들지 못했어요"
internal const val IMPORT_PARTIAL_MESSAGE = "일부 장소를 못 넣었어요"
internal const val IMPORT_FAILED_MESSAGE = "불러온 장소를 넣지 못했어요"

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
    private val placeImportRepository: PlaceImportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(savedStateHandle.toCreateMapUiState())
    val uiState: StateFlow<CreateMapUiState> = _uiState.asStateFlow()

    /**
     * 커버 사진을 고른다.
     *
     * 올리는 중에는 받지 않는다. 이미 시작한 업로드는 그대로 끝나므로, 바꾸게 두면 미리보기에는
     * 새 사진이 뜨는데 지도에는 앞의 사진이 저장된다.
     */
    fun selectImage(uri: String) {
        if (_uiState.value.isSubmitting) return
        updateState { state -> state.copy(imageUri = uri) }
    }

    /** 고른 커버 사진을 뺀다. 올리는 중에는 받지 않는다 - [selectImage] 와 같은 이유다. */
    fun removeImage() {
        if (_uiState.value.isSubmitting) return
        updateState { state -> state.copy(imageUri = null) }
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
     * 태그 입력값 변경. 플레이스홀더가 안내하는 대로 스페이스나 엔터가 들어오면 그 앞까지를 태그로
     * 확정한다(장소 추가와 같은 [applyTagInput]). 붙여넣기로 여러 개가 한 번에 들어와도 같다.
     */
    fun updateTagInput(input: String) {
        updateState { state ->
            val result = applyTagInput(state.tags, input)
            state.copy(tags = result.tags, tagInput = result.input.take(TAG_MAX_LENGTH))
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

    fun setMemberAutoApproval(enabled: Boolean) {
        updateState { state -> state.copy(memberAutoApproval = enabled) }
    }

    /**
     * 외부 지도에서 불러온 장소를 받는다. 앞서 불러온 것이 있으면 **바꾼다** - 덧붙이면 같은
     * 리스트를 다시 불러왔을 때 장소가 두 번 들어간다.
     *
     * 만드는 중에는 받지 않는다. 이미 나간 요청에는 실리지 않으므로 받아도 버려진다.
     */
    fun applyImportedPlaces(places: List<EditedPlace>) {
        if (_uiState.value.submit != SubmitState.Idle) return
        updateState { state -> state.copy(importedPlaces = places) }
    }

    /**
     * 커버를 올리고 지도를 만든다.
     *
     * 사진은 **여기서** 올린다. 고를 때마다 올리면 만들기를 그만둔 사용자의 사진이 스토리지에
     * 남는데 지울 방법이 없다. 업로드가 실패하면 지도도 만들지 않는다 - 커버 없이 만들어지면
     * 사용자는 성공한 줄 알고 나가고, 지도 수정 화면이 없어 나중에 붙일 방법도 없다.
     *
     * 불러온 장소는 지도가 생긴 **뒤에** 넣는다. 사진 발급이 `mapId` 를 요구한다. 장소를 다 넣지
     * 못해도 지도는 그대로 두고 끝까지 간다 - 지도는 이미 만들어졌다. 안내는 결과에 실어
     * 돌아간 모음 탭이 띄운다.
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
            memberAutoApproval = current.memberAutoApproval,
        )

        // 코루틴이 시작되기를 기다리지 않고 여기서 잠근다. 시작 시점에 잠그면 그 전에
        // 들어온 두 번째 탭이 가드를 그대로 통과해 지도가 두 개 만들어진다.
        updateState { state ->
            state.copy(submit = SubmitState.Submitting, errorMessage = null)
        }

        viewModelScope.launch {
            val imageUrl = try {
                current.imageUri?.let { uri -> uploadCover(uri) }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.w(TAG, "커버 이미지 업로드 실패", throwable)
                // 지도를 만들지 않고 멈춘다. 입력값은 그대로 둔다.
                updateState { state ->
                    state.copy(
                        submit = SubmitState.Idle,
                        errorMessage = throwable.toUploadMessage(),
                    )
                }
                return@launch
            }

            val created = try {
                mapRepository.createMap(newMap.copy(imageUrl = imageUrl))
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.e(TAG, "지도 생성 실패", throwable)
                // 입력값은 그대로 둔다. 이름·설명·태그를 다시 치게 만들면 안 된다.
                updateState { state ->
                    state.copy(
                        submit = SubmitState.Idle,
                        errorMessage = throwable.toUserMessage(CREATE_FAILED_MESSAGE),
                    )
                }
                return@launch
            }

            val notice = current.importedPlaces
                .takeIf { places -> places.isNotEmpty() }
                ?.let { places -> importPlaces(created.id, places) }
            // 프라이빗 지도는 초대 코드를 먼저 보여주고, 닫을 때 화면을 뺀다.
            val next = created.inviteCode
                ?.let { code -> SubmitState.ShowingInviteCode(created.id, code, notice) }
                ?: SubmitState.Done(created.id, notice)
            updateState { state -> state.copy(submit = next) }
        }
    }

    /**
     * 만든 지도에 불러온 장소를 넣고, 다 넣지 못했으면 안내 문구를 돌려준다.
     *
     * 사진이 없는 장소는 저장소가 알아서 건너뛴다. 추출이 100곳까지라 일괄 등록도 한 번에 끝난다.
     */
    private suspend fun importPlaces(mapId: Long, places: List<EditedPlace>): String? {
        val result = try {
            val photoUrls = placeImportRepository.uploadPhotos(mapId, places)
            placeImportRepository.savePlaces(setOf(mapId), places, photoUrls)
        } catch (e: CancellationException) {
            throw e
        } catch (throwable: Throwable) {
            Log.w(TAG, "불러온 장소 등록 실패", throwable)
            return IMPORT_FAILED_MESSAGE
        }

        return when {
            result.failed == 0 -> null
            result.created == 0 && result.duplicate == 0 -> IMPORT_FAILED_MESSAGE
            else -> IMPORT_PARTIAL_MESSAGE
        }
    }

    /**
     * 커버를 올리고 주소를 기억한다.
     *
     * 앞선 시도에서 같은 사진을 이미 올렸으면 그 주소를 그대로 쓴다. 생성만 실패해 다시 눌렀을
     * 때 매번 새로 올리면 지울 수 없는 사진이 시도할 때마다 쌓인다.
     */
    private suspend fun uploadCover(uri: String): String =
        _uiState.value.reusableCoverUrl ?: mapRepository.uploadCoverImage(uri).also { fileUrl ->
            updateState { state -> state.copy(uploadedCover = UploadedCover(uri, fileUrl)) }
        }

    /** 초대 코드를 다 본 뒤. 지도는 이미 만들어졌으므로 그대로 화면을 뺀다. */
    fun dismissInviteCode() {
        val showing = _uiState.value.submit as? SubmitState.ShowingInviteCode ?: return
        updateState { state ->
            state.copy(submit = SubmitState.Done(showing.mapId, showing.importNotice))
        }
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
private const val KEY_COVER_SOURCE_URI = "createMap.coverSourceUri"
private const val KEY_COVER_FILE_URL = "createMap.coverFileUrl"
private const val KEY_NAME = "createMap.name"
private const val KEY_DESCRIPTION = "createMap.description"
private const val KEY_VISIBILITY = "createMap.visibility"
private const val KEY_TAGS = "createMap.tags"
private const val KEY_TAG_INPUT = "createMap.tagInput"
private const val KEY_MEMBER_AUTO_APPROVAL = "createMap.memberAutoApproval"
private const val KEY_CREATED_MAP_ID = "createMap.createdMapId"
private const val KEY_INVITE_CODE = "createMap.inviteCode"
private const val KEY_IMPORTED_PLACES = "createMap.importedPlaces"
private const val KEY_IMPORT_NOTICE = "createMap.importNotice"

/**
 * 저장된 값에서 상태를 되살린다.
 *
 * [CreateMapUiState] 를 통째로 넣지 않고 항목별로 쪼개 둔다 - Bundle 에 담으려면 Parcelable
 * 이어야 하는데, 그러자고 화면 상태에 직렬화 형식을 끌어들일 이유가 없다.
 */
private fun SavedStateHandle.toCreateMapUiState() = CreateMapUiState(
    imageUri = get<String>(KEY_IMAGE_URI),
    // 올려둔 커버는 되살린다. 스토리지에 이미 올라간 사진이라, 잊어버리면 같은 사진이 하나 더
    // 올라가고 앞의 것은 지울 방법이 없다.
    uploadedCover = restoreUploadedCover(),
    name = get<String>(KEY_NAME).orEmpty(),
    description = get<String>(KEY_DESCRIPTION).orEmpty(),
    // 저장한 뒤 enum 이 바뀌었을 수 있으니 모르는 값은 고르지 않은 것으로 본다.
    visibility = get<String>(KEY_VISIBILITY)?.let { saved ->
        MapVisibility.entries.firstOrNull { it.name == saved }
    },
    tags = get<ArrayList<String>>(KEY_TAGS).orEmpty(),
    tagInput = get<String>(KEY_TAG_INPUT).orEmpty(),
    memberAutoApproval = get<Boolean>(KEY_MEMBER_AUTO_APPROVAL) ?: false,
    importedPlaces = get<String>(KEY_IMPORTED_PLACES)?.let(::decodeImportedPlaces).orEmpty(),
    // 진행 중 상태는 복원하지 않는다. 요청은 프로세스와 함께 사라졌는데 "만드는 중" 으로
    // 되살아나면 버튼이 영영 잠긴다.
    //
    // 초대 코드는 되살린다. 지도는 이미 만들어졌고, 이 화면을 벗어나면 코드를 다시 볼
    // 방법이 없다. 화면이 빠지기 전에 죽었어도 만든 지도로 되살려 다시 만들 수 없게 하고,
    // 모음 탭에 넘길 장소 안내도 함께 되살린다.
    submit = restoreCreatedMap() ?: SubmitState.Idle,
)

private fun SavedStateHandle.restoreUploadedCover(): UploadedCover? {
    val sourceUri = get<String>(KEY_COVER_SOURCE_URI)?.takeIf { it.isNotBlank() } ?: return null
    val fileUrl = get<String>(KEY_COVER_FILE_URL)?.takeIf { it.isNotBlank() } ?: return null
    return UploadedCover(sourceUri, fileUrl)
}

private fun SavedStateHandle.restoreCreatedMap(): SubmitState? {
    val mapId = get<Long>(KEY_CREATED_MAP_ID) ?: return null
    val notice = get<String>(KEY_IMPORT_NOTICE)
    val code = get<String>(KEY_INVITE_CODE)?.takeIf { it.isNotBlank() }
        ?: return SubmitState.Done(mapId, notice)
    return SubmitState.ShowingInviteCode(mapId, code, notice)
}

private fun SavedStateHandle.save(state: CreateMapUiState) {
    this[KEY_IMAGE_URI] = state.imageUri
    this[KEY_COVER_SOURCE_URI] = state.uploadedCover?.sourceUri
    this[KEY_COVER_FILE_URL] = state.uploadedCover?.fileUrl
    this[KEY_NAME] = state.name
    this[KEY_DESCRIPTION] = state.description
    this[KEY_VISIBILITY] = state.visibility?.name
    // Bundle 이 담을 수 있는 형태여야 해서 ArrayList 로 넘긴다.
    this[KEY_TAGS] = ArrayList(state.tags)
    this[KEY_TAG_INPUT] = state.tagInput
    this[KEY_MEMBER_AUTO_APPROVAL] = state.memberAutoApproval
    this[KEY_IMPORTED_PLACES] = state.importedPlaces
        .takeIf { places -> places.isNotEmpty() }
        ?.let(::encodeImportedPlaces)

    when (val submit = state.submit) {
        is SubmitState.ShowingInviteCode -> {
            this[KEY_CREATED_MAP_ID] = submit.mapId
            this[KEY_INVITE_CODE] = submit.inviteCode
            this[KEY_IMPORT_NOTICE] = submit.importNotice
        }

        is SubmitState.Done -> {
            this[KEY_CREATED_MAP_ID] = submit.mapId
            this[KEY_INVITE_CODE] = null
            this[KEY_IMPORT_NOTICE] = submit.importNotice
        }

        else -> {
            this[KEY_CREATED_MAP_ID] = null
            this[KEY_INVITE_CODE] = null
            this[KEY_IMPORT_NOTICE] = null
        }
    }
}

/** 빈 값과 이미 담긴 태그는 걸러낸다. 한 번에 들어온 값들 사이의 중복도 마찬가지다. */
internal fun List<String>.plusTags(candidates: List<String>): List<String> {
    val added = candidates
        .map { candidate -> candidate.trim().take(TAG_MAX_LENGTH) }
        .filter { candidate -> candidate.isNotEmpty() }

    return (this + added).distinct()
}

