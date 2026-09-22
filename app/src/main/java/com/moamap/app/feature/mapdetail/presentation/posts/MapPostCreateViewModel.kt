package com.moamap.app.feature.mapdetail.presentation.posts

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.upload.ImageUploadException
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.domain.model.MapPostPlaceTag
import com.moamap.app.feature.mapdetail.domain.model.NewMapPost
import com.moamap.app.feature.mapdetail.domain.repository.MapPostRepository
import com.moamap.app.feature.mapdetail.presentation.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MapPostCreateViewModel"

/** 서버가 게시물 하나에 받는 한도. */
internal const val MAX_POST_PHOTOS = 5
internal const val MAX_POST_CONTENT_LENGTH = 1000

/**
 * 게시물에 태그할 장소 수.
 *
 * 서버는 10개까지 받지만 시안은 한 곳만 보여준다. 화면 기준을 따라 하나로 막는다 -
 * 여러 곳을 태그하면 카드에 무엇을 띄울지부터 정해져 있지 않다.
 */
internal const val MAX_POST_PLACE_TAGS = 1

internal const val POST_PHOTO_UPLOAD_FAILED_MESSAGE = "사진을 올리지 못했어요"
internal const val POST_CREATE_FAILED_MESSAGE = "게시물을 올리지 못했어요"

/** 게시물에 태그한 장소. 폼에서는 주소까지 보여준다. */
@Immutable
data class SelectedPostPlace(
    val placeId: Long,
    val name: String,
    val address: String,
)

@Immutable
data class MapPostCreateUiState(
    val content: String = "",
    val photos: List<Uri> = emptyList(),
    /**
     * 앞선 시도에서 이미 올려 둔 사진 주소. [photos] 와 같은 순서다.
     *
     * 작성만 실패해 다시 누를 때 사진을 또 올리지 않으려고 둔다. 사진 목록이 바뀌면 비운다.
     */
    val uploadedPhotoUrls: List<String> = emptyList(),
    val places: List<SelectedPostPlace> = emptyList(),
    val submitting: Boolean = false,
    val errorMessage: String? = null,
    /** 올리기에 성공한 횟수. 화면이 이 값이 바뀌는 것을 보고 닫는다. */
    val postedCount: Int = 0,
) {
    val canAddPhoto: Boolean get() = photos.size < MAX_POST_PHOTOS

    val canAddPlace: Boolean get() = places.size < MAX_POST_PLACE_TAGS

    /** 본문이 비면 올릴 수 없다. 서버가 수정할 때 빈 본문을 막아, 작성에서도 같은 기준을 둔다. */
    val canSubmit: Boolean get() = content.isNotBlank() && !submitting
}

/**
 * 지도 로그 탭의 새 게시물.
 *
 * 지도 상세 화면에 매여 있어 폼을 닫아도 살아남는다. 새로 열 때 [reset] 으로 비운다.
 */
@HiltViewModel
class MapPostCreateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MapPostRepository,
) : ViewModel() {

    private val mapId: Long = checkNotNull(savedStateHandle[MoaMapRoute.MapDetail.ARG_MAP_ID]) {
        "게시물 작성은 ${MoaMapRoute.MapDetail.ARG_MAP_ID} 없이 열 수 없다"
    }

    private val _uiState = MutableStateFlow(MapPostCreateUiState())
    val uiState: StateFlow<MapPostCreateUiState> = _uiState.asStateFlow()

    private var submitJob: Job? = null

    /** 성공 횟수는 남긴다. 되돌리면 화면이 "새로 성공했다" 는 신호를 놓친다. */
    fun reset() {
        submitJob?.cancel()
        _uiState.update { state -> MapPostCreateUiState(postedCount = state.postedCount) }
    }

    /** 한도를 넘는 입력은 잘라 낸다. 붙여 넣기로 한 번에 넘길 수 있다. */
    fun updateContent(content: String) {
        _uiState.update { state -> state.copy(content = content.take(MAX_POST_CONTENT_LENGTH)) }
    }

    fun addPhoto(uri: Uri) {
        _uiState.update { state ->
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

    fun addPlace(place: MapPlace) {
        _uiState.update { state ->
            if (!state.canAddPlace || state.places.any { it.placeId == place.id }) state
            else state.copy(
                places = state.places + SelectedPostPlace(
                    placeId = place.id,
                    name = place.name,
                    address = place.address,
                ),
            )
        }
    }

    fun removePlace(placeId: Long) {
        _uiState.update { state -> state.copy(places = state.places.filterNot { it.placeId == placeId }) }
    }

    fun consumeErrorMessage() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    /**
     * 게시물을 올린다.
     *
     * 사진은 **여기서** 올린다. 고를 때마다 올리면 작성을 그만둔 사용자의 사진이 서버에 남는다.
     * 사진이 하나라도 실패하면 게시물을 올리지 않는다 - 사진이 빠진 채로 올라가면 사용자가 알 수 없다.
     */
    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { current -> current.copy(submitting = true, errorMessage = null) }

        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            // 앞선 시도에서 이미 올렸으면 다시 올리지 않는다. 매번 새로 올리면 지울 수 없는 사진이 쌓인다.
            val photoUrls = state.uploadedPhotoUrls.ifEmpty {
                try {
                    repository.uploadPhotos(mapId, state.photos).also { uploaded ->
                        _uiState.update { current -> current.copy(uploadedPhotoUrls = uploaded) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "게시물 사진 업로드 실패 (mapId=$mapId)", e)
                    fail(e.toPhotoMessage())
                    return@launch
                }
            }

            try {
                repository.createPost(
                    mapId = mapId,
                    post = NewMapPost(
                        content = state.content.trim(),
                        photoUrls = photoUrls,
                        placeTags = state.places.map { place ->
                            MapPostPlaceTag(placeId = place.placeId, name = place.name)
                        },
                    ),
                )
                _uiState.update { current ->
                    MapPostCreateUiState(postedCount = current.postedCount + 1)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "게시물 작성 실패 (mapId=$mapId)", e)
                fail(e.toUserMessage(POST_CREATE_FAILED_MESSAGE))
            }
        }
    }

    /** 실패해도 폼을 비우지 않는다. 적어 둔 것이 날아가면 안 된다. */
    private fun fail(message: String) {
        _uiState.update { state -> state.copy(submitting = false, errorMessage = message) }
    }
}

/**
 * 형식이나 크기 때문에 걸린 것은 사용자가 사진을 바꿔야 하는 일이라, 예외가 들고 있는 이유를
 * 그대로 보여준다. "사진을 올리지 못했어요" 로 뭉개면 무엇을 고쳐야 하는지 알 수 없다.
 */
private fun Throwable.toPhotoMessage(): String =
    if (this is ImageUploadException) message ?: POST_PHOTO_UPLOAD_FAILED_MESSAGE
    else toUserMessage(POST_PHOTO_UPLOAD_FAILED_MESSAGE)
