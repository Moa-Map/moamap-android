package com.moamap.app.feature.mapdetail.presentation.info

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.collection.domain.repository.MapRepository
import com.moamap.app.feature.collection.presentation.createmap.DESCRIPTION_MAX_LENGTH
import com.moamap.app.feature.collection.presentation.createmap.NAME_MAX_LENGTH
import com.moamap.app.feature.collection.presentation.createmap.TAG_MAX_LENGTH
import com.moamap.app.feature.collection.presentation.createmap.UploadedCover
import com.moamap.app.feature.collection.presentation.createmap.plusTags
import com.moamap.app.feature.collection.presentation.createmap.toCoverMessage
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MapInfoEditViewModel"
private const val SAVE_FAILED_MESSAGE = "지도 정보를 저장하지 못했어요"
private const val NETWORK_ERROR_MESSAGE = "네트워크에 연결할 수 없어요"

/**
 * 고치는 중인 지도 정보. [original] 에서 시작하고, 하나라도 달라져야 저장할 수 있다.
 */
@Immutable
internal data class MapInfoForm(
    val original: MapDetail,
    val name: String = original.title,
    val description: String = original.description.orEmpty(),
    val tags: List<String> = original.tags,
    /** 아직 확정하지 않은 태그 입력값. 엔터로 확정한다. */
    val tagInput: String = "",
    /** 새로 고른 사진의 `content://`. 고르지 않았으면 null 이고 원래 사진을 그대로 둔다. */
    val newPhotoUri: String? = null,
) {
    /** 사진 칸에 띄울 사진. 새로 고른 사진이 먼저다. */
    val photo: String?
        get() = newPhotoUri ?: original.imageUrl

    /**
     * 처음 값과 달라졌는지. 고쳤다가 되돌리면 다시 false 다.
     *
     * 확정하지 않은 태그 입력도 바뀐 것으로 본다 - 저장할 때 태그로 확정해 함께 보낸다.
     */
    val changed: Boolean
        get() = newPhotoUri != null ||
            name != original.title ||
            description != original.description.orEmpty() ||
            tags != original.tags ||
            tagInput.isNotBlank()
}

@Immutable
internal data class MapInfoEditUiState(
    /** 수정 화면이 열려 있으면 값이 있다. */
    val form: MapInfoForm? = null,
    val saving: Boolean = false,
    /** 이미 올려둔 사진. 저장만 실패해 다시 누를 때 같은 사진을 또 올리지 않으려고 붙잡는다. */
    val uploadedCover: UploadedCover? = null,
    val errorMessage: String? = null,
    /** 저장에 성공한 횟수. 지도 상세는 이 값이 오를 때 지도를 다시 읽는다. */
    val savedCount: Int = 0,
) {
    /** 이름은 서버 필수값이라 비우면 저장할 수 없다. */
    val canSave: Boolean
        get() = form != null && form.changed && form.name.isNotBlank() && !saving
}

/**
 * 지도 정보 수정. 지도 상세 위에 덮이는 화면이라 지도 상세와 같은 자리에서 산다.
 *
 * 열 때마다 그 순간의 지도 값으로 새로 시작한다([open]).
 */
@HiltViewModel
internal class MapInfoEditViewModel @Inject constructor(
    private val mapRepository: MapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapInfoEditUiState())
    val uiState: StateFlow<MapInfoEditUiState> = _uiState.asStateFlow()

    fun open(map: MapDetail) {
        _uiState.update { state -> MapInfoEditUiState(form = MapInfoForm(map), savedCount = state.savedCount) }
    }

    /**
     * 저장하지 않고 닫는다.
     *
     * 저장 중에는 닫지 않는다. 요청은 이미 서버로 갔을 수 있어, 닫고 나서 결과가 오면 지도 상세가
     * 바뀐 값을 놓치거나 다시 연 화면을 덮어쓴다. 실패해도 통신 시간 제한으로 곧 풀린다.
     */
    fun close() {
        if (_uiState.value.saving) return
        _uiState.update { state -> MapInfoEditUiState(savedCount = state.savedCount) }
    }

    /** 올리는 중에는 받지 않는다. 미리보기만 바뀌고 앞의 사진이 저장된다. */
    fun selectPhoto(uri: String) {
        if (_uiState.value.saving) return
        updateForm { form -> form.copy(newPhotoUri = uri) }
    }

    fun updateName(name: String) {
        updateForm { form -> form.copy(name = name.take(NAME_MAX_LENGTH)) }
    }

    fun updateDescription(description: String) {
        updateForm { form -> form.copy(description = description.take(DESCRIPTION_MAX_LENGTH)) }
    }

    /** 스페이스로는 확정하지 않는다. 시안 「태그 입력 후 엔터」대로 띄어쓰기도 태그에 들어간다. */
    fun updateTagInput(input: String) {
        updateForm { form -> form.copy(tagInput = input.take(TAG_MAX_LENGTH)) }
    }

    /** 엔터로 확정할 때 쓴다. */
    fun commitTag() {
        updateForm { form -> form.copy(tags = form.tags.plusTags(listOf(form.tagInput)), tagInput = "") }
    }

    fun removeTag(tag: String) {
        updateForm { form -> form.copy(tags = form.tags - tag) }
    }

    fun consumeError() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    /**
     * 새 사진을 골랐으면 먼저 올리고, 이름·설명·사진·태그를 저장한다.
     *
     * 서버가 통째로 덮어써서, 고치지 않은 값도 처음 값 그대로 함께 보낸다. 성공하면 화면을 닫는다.
     */
    fun save() {
        if (!_uiState.value.canSave) return

        // 엔터 없이 입력만 해 둔 태그도 확정한다. 화면의 칩과 보내는 값이 어긋나지 않게 상태부터.
        commitTag()
        val form = _uiState.value.form ?: return

        // 코루틴 시작을 기다리지 않고 잠근다. 그 사이 들어온 두 번째 탭이 가드를 통과하지 않게.
        _uiState.update { state -> state.copy(saving = true, errorMessage = null) }

        viewModelScope.launch {
            val imageUrl = try {
                form.newPhotoUri?.let { uri -> uploadCover(uri) } ?: form.original.imageUrl
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.w(TAG, "지도 사진 업로드 실패", throwable)
                // 정보도 저장하지 않는다. 사진만 빠진 채 저장되면 성공한 줄 알고 나간다.
                _uiState.update { state ->
                    state.copy(saving = false, errorMessage = throwable.toCoverMessage())
                }
                return@launch
            }

            try {
                mapRepository.updateMap(
                    mapId = form.original.id,
                    name = form.name,
                    description = form.description,
                    imageUrl = imageUrl,
                    tags = form.tags,
                )
                _uiState.update { state -> MapInfoEditUiState(savedCount = state.savedCount + 1) }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.e(TAG, "지도 정보 저장 실패", throwable)
                // 입력값은 그대로 둔다. 다시 누르면 된다.
                _uiState.update { state ->
                    state.copy(saving = false, errorMessage = throwable.toSaveMessage())
                }
            }
        }
    }

    /** 같은 사진을 앞선 시도에서 이미 올렸으면 그 주소를 쓴다. 지울 수 없는 사진이 쌓이지 않게. */
    private suspend fun uploadCover(uri: String): String =
        _uiState.value.uploadedCover?.takeIf { it.sourceUri == uri }?.fileUrl
            ?: mapRepository.uploadCoverImage(uri).also { fileUrl ->
                _uiState.update { state -> state.copy(uploadedCover = UploadedCover(uri, fileUrl)) }
            }

    private fun updateForm(transform: (MapInfoForm) -> MapInfoForm) {
        _uiState.update { state -> state.form?.let { form -> state.copy(form = transform(form)) } ?: state }
    }
}

private fun Throwable.toSaveMessage(): String = when (this) {
    is ConnectionException -> NETWORK_ERROR_MESSAGE
    else -> SAVE_FAILED_MESSAGE
}
