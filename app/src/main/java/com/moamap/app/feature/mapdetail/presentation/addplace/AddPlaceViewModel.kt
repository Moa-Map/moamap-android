package com.moamap.app.feature.mapdetail.presentation.addplace

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.upload.ImageUploadException
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.NewPlace
import com.moamap.app.feature.mapdetail.domain.model.PlaceCandidate
import com.moamap.app.feature.mapdetail.domain.repository.PlaceAddRepository
import com.moamap.app.feature.mapdetail.domain.repository.PlaceSearchRepository
import com.moamap.app.feature.mapdetail.presentation.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AddPlaceViewModel"

private const val SEARCH_FAILED_MESSAGE = "장소를 검색하지 못했어요"
private const val PHOTO_UPLOAD_FAILED_MESSAGE = "사진을 올리지 못했어요"
private const val ADD_FAILED_MESSAGE = "장소를 추가하지 못했어요"
private const val DUPLICATE_MESSAGE = "이미 이 지도에 있는 장소예요"

/** `[409] PLACE_010: 해당 지도에 이미 등록된 장소입니다.` */
private const val DUPLICATE_PLACE_CODE = "PLACE_010"

/**
 * 등록 실패 안내.
 *
 * 중복만 따로 가른다. "추가하지 못했어요" 하나로 묶으면 왜 안 되는지 알 수 없어 같은
 * 장소를 계속 다시 시도하게 된다. 그 밖의 경우는 원인을 단정하지 않는다.
 */
private fun Throwable.toAddPlaceMessage(): String =
    if (this is ApiException && code == DUPLICATE_PLACE_CODE) DUPLICATE_MESSAGE
    else toUserMessage(ADD_FAILED_MESSAGE)

/**
 * 사진 업로드 실패는 등록 실패와 다르게 안내한다.
 *
 * 형식이나 크기 때문에 걸린 것은 사용자가 사진을 바꿔야 하는 일이라, 예외가 들고 있는 이유를
 * 그대로 보여준다. "사진을 올리지 못했어요" 로 뭉개면 무엇을 고쳐야 하는지 알 수 없다.
 */
private fun Throwable.toPhotoMessage(): String =
    if (this is ImageUploadException) message ?: PHOTO_UPLOAD_FAILED_MESSAGE
    else toUserMessage(PHOTO_UPLOAD_FAILED_MESSAGE)

/**
 * 검색어를 치는 동안 기다리는 시간.
 *
 * 글자마다 부르면 카카오 쿼터를 태운다. 쿼터가 소진되면 인증 실패가 아니라 200 에 빈
 * 결과로 와서 원인을 알아채기 어렵다.
 */
private const val SEARCH_DEBOUNCE_MILLIS = 300L

@HiltViewModel
class AddPlaceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchRepository: PlaceSearchRepository,
    private val addRepository: PlaceAddRepository,
) : ViewModel() {

    private val mapId: Long = checkNotNull(savedStateHandle[MoaMapRoute.MapDetail.ARG_MAP_ID]) {
        "장소 추가는 ${MoaMapRoute.MapDetail.ARG_MAP_ID} 없이 열 수 없다"
    }

    private val _uiState = MutableStateFlow(AddPlaceUiState())
    val uiState: StateFlow<AddPlaceUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var submitJob: Job? = null

    // ---------- 1단계: 검색 ----------

    fun updateQuery(query: String) {
        _uiState.update { state -> state.copy(query = query) }

        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { state -> state.copy(search = PlaceSearchState.Idle) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            _uiState.update { state -> state.copy(search = PlaceSearchState.Loading) }
            search(query)
        }
    }

    fun retrySearch() {
        val query = _uiState.value.query
        if (query.isBlank()) return

        searchJob?.cancel()
        _uiState.update { state -> state.copy(search = PlaceSearchState.Loading) }
        searchJob = viewModelScope.launch { search(query) }
    }

    private suspend fun search(query: String) {
        val next = try {
            PlaceSearchState.Success(searchRepository.search(query))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "장소 검색 실패", e)
            PlaceSearchState.Error(e.toUserMessage(SEARCH_FAILED_MESSAGE))
        }
        _uiState.update { state -> state.copy(search = next) }
    }

    /**
     * 시트를 새로 열 때 처음 상태로 되돌린다.
     *
     * 이 ViewModel 은 지도 상세 화면에 매여 있어 시트를 닫아도 살아남는다. 지우지 않으면
     * 다시 열었을 때 직전에 등록한 장소의 폼이 그대로 보인다.
     */
    fun reset() {
        searchJob?.cancel()
        submitJob?.cancel()
        _uiState.value = AddPlaceUiState()
    }

    // ---------- 단계 이동 ----------

    fun selectCandidate(candidate: PlaceCandidate) {
        _uiState.update { state -> state.copy(selected = candidate) }
    }

    /** 검색으로 돌아간다. 검색 결과는 남겨 두고 폼에 적은 것만 버린다. */
    fun backToSearch() {
        _uiState.update { state ->
            state.copy(
                selected = null,
                photos = emptyList(),
                uploadedPhotoUrls = emptyList(),
                tags = emptyList(),
                tagInput = "",
                memo = "",
            )
        }
    }

    // ---------- 2단계: 등록 폼 ----------

    fun addPhoto(uri: Uri) {
        _uiState.update { state ->
            // 이미 꽉 찼거나 같은 사진이면 무시한다.
            if (!state.canAddPhoto || uri in state.photos) state
            // 목록이 바뀌면 앞서 올려 둔 주소는 더 이상 이 목록과 맞지 않는다.
            else state.copy(photos = state.photos + uri, uploadedPhotoUrls = emptyList())
        }
    }

    fun removePhoto(uri: Uri) {
        _uiState.update { state ->
            state.copy(photos = state.photos - uri, uploadedPhotoUrls = emptyList())
        }
    }

    fun updateTagInput(input: String) {
        _uiState.update { state ->
            val result = applyTagInput(tags = state.tags, rawInput = input)
            state.copy(tags = result.tags, tagInput = result.input)
        }
    }

    /** 입력창이 비었을 때 백스페이스를 누르면 마지막 태그를 지운다. */
    fun removeLastTagIfInputEmpty() {
        _uiState.update { state ->
            if (state.tagInput.isNotEmpty()) state
            else state.copy(tags = removeLastTag(state.tags))
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { state -> state.copy(tags = state.tags - tag) }
    }

    fun updateMemo(memo: String) {
        _uiState.update { state -> state.copy(memo = memo) }
    }

    fun consumeErrorMessage() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    /**
     * 장소를 등록한다.
     *
     * 사진은 **여기서** 올린다. 고를 때마다 올리면 등록을 그만둔 사용자의 사진이 서버에
     * 남는다. 사진이 하나라도 실패하면 등록하지 않는다 - 사진이 빠진 채로 등록되면
     * 사용자가 알 수 없다.
     */
    fun submit(map: MapDetail) {
        val state = _uiState.value
        val candidate = state.selected ?: return
        if (state.submitting) return

        _uiState.update { current -> current.copy(submitting = true, errorMessage = null) }

        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            // 앞선 시도에서 이미 올렸으면 다시 올리지 않는다. 등록만 실패해 다시 눌렀을 때
            // 매번 새로 올리면 지울 수 없는 사진이 시도할 때마다 쌓인다.
            val photoUrls = state.uploadedPhotoUrls.ifEmpty {
                try {
                    addRepository.uploadPhotos(mapId, state.photos).also { uploaded ->
                        _uiState.update { current -> current.copy(uploadedPhotoUrls = uploaded) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "사진 업로드 실패 (mapId=$mapId)", e)
                    fail(e.toPhotoMessage())
                    return@launch
                }
            }

            try {
                addRepository.addPlace(
                    mapId = mapId,
                    newPlace = NewPlace(
                        candidate = candidate,
                        tags = state.tags,
                        memo = state.memo,
                        photoUrls = photoUrls,
                    ),
                )
                _uiState.update { current ->
                    current.copy(submitting = false, addedMessage = addPlaceDoneMessage(map))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "장소 등록 실패 (mapId=$mapId)", e)
                fail(e.toAddPlaceMessage())
            }
        }
    }

    /** 실패해도 시트를 닫지 않는다. 적어 둔 것이 날아가면 안 된다. */
    private fun fail(message: String) {
        _uiState.update { state -> state.copy(submitting = false, errorMessage = message) }
    }
}
