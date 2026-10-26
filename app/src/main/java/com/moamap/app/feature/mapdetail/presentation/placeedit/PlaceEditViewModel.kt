package com.moamap.app.feature.mapdetail.presentation.placeedit

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.PLACE_UPDATED_TOAST
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.domain.repository.MapDetailRepository
import com.moamap.app.feature.mapdetail.presentation.addplace.applyTagInput
import com.moamap.app.feature.mapdetail.presentation.addplace.removeLastTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PlaceEditViewModel"
private const val SAVE_FAILED_MESSAGE = "장소 정보를 수정하지 못했어요"
private const val DELETE_FAILED_MESSAGE = "장소를 삭제하지 못했어요"

/**
 * 고치는 중인 장소. [original] 에서 시작하고, 하나라도 달라져야 저장할 수 있다.
 *
 * 메모·태그만 고친다. 서버가 사진을 바꾸지 못하고, 이름·주소는 시안에서 보여 주기만 한다.
 */
@Immutable
internal data class PlaceEditForm(
    val original: MapPlace,
    val tags: List<String> = original.tags,
    /** 아직 확정하지 않은 태그 입력값. 스페이스·엔터로 확정한다. */
    val tagInput: String = "",
    val memo: String = original.description,
) {
    /**
     * 처음 값과 달라졌는지. 고쳤다가 되돌리면 다시 false 다.
     *
     * 확정하지 않은 태그 입력도 바뀐 것으로 본다 - 저장할 때 태그로 확정해 함께 보낸다.
     */
    val changed: Boolean
        get() = tags != original.tags || memo != original.description || tagInput.isNotBlank()
}

@Immutable
internal data class PlaceEditUiState(
    /** 수정 화면이 열려 있으면 값이 있다. */
    val form: PlaceEditForm? = null,
    val saving: Boolean = false,
    val errorMessage: String? = null,
    /** 고친 채로 뒤로 가려 할 때 「수정을 그만둘까요?」를 띄운다. */
    val discardConfirmVisible: Boolean = false,
    /** 저장으로 바뀐 장소. 지도 상세가 목록에 넣고 비운다. */
    val savedPlace: MapPlace? = null,
    /** 저장하고 돌아간 장소 페이지가 한 번 띄우는 완료 토스트. */
    val toast: String? = null,
    val deleting: Boolean = false,
    /** 장소 페이지에서 지우다 실패한 안내. 수정 화면이 없을 때라 [errorMessage] 와 따로 든다. */
    val deleteErrorMessage: String? = null,
    /** 지운 장소. 지도 상세가 장소 페이지를 닫고 목록을 다시 읽은 뒤 비운다. */
    val deletedPlaceId: Long? = null,
) {
    val canSave: Boolean
        get() = form != null && form.changed && !saving
}

/**
 * 장소 수정·삭제. 장소 페이지 ⋮ 메뉴에서 들어오고, 지도 상세 위에 덮이는 화면이라 지도 상세와 같은
 * 자리에서 산다. 열 때마다 그 순간의 장소 값으로 새로 시작한다([open]).
 */
@HiltViewModel
internal class PlaceEditViewModel @Inject constructor(
    private val repository: MapDetailRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceEditUiState())
    val uiState: StateFlow<PlaceEditUiState> = _uiState.asStateFlow()

    fun open(place: MapPlace) {
        _uiState.update { state ->
            state.copy(form = PlaceEditForm(place), saving = false, errorMessage = null, discardConfirmVisible = false)
        }
    }

    /** `←`·기기 뒤로가기. 바뀐 게 없으면 바로 닫고, 있으면 그만둘지 묻는다(시안 「장소 수정 관련」). */
    fun requestClose() {
        val state = _uiState.value
        if (state.saving) return
        if (state.form?.changed == true) {
            _uiState.update { current -> current.copy(discardConfirmVisible = true) }
        } else {
            close()
        }
    }

    /** 「계속 수정」. */
    fun keepEditing() {
        _uiState.update { state -> state.copy(discardConfirmVisible = false) }
    }

    /**
     * 저장하지 않고 닫는다. 그만둘지 물은 팝업의 「나가기」도 여기로 온다.
     *
     * 저장 중에는 닫지 않는다. 요청은 이미 서버로 갔을 수 있어, 닫고 나서 결과가 오면 다시 연 화면을
     * 덮어쓴다. 실패해도 통신 시간 제한으로 곧 풀린다.
     */
    fun close() {
        if (_uiState.value.saving) return
        _uiState.update { state -> state.copy(form = null, errorMessage = null, discardConfirmVisible = false) }
    }

    fun updateTagInput(input: String) {
        updateForm { form ->
            val result = applyTagInput(tags = form.tags, rawInput = input)
            form.copy(tags = result.tags, tagInput = result.input)
        }
    }

    /** 입력창이 비었을 때 백스페이스를 누르면 마지막 태그를 지운다. */
    fun removeLastTagIfInputEmpty() {
        updateForm { form -> if (form.tagInput.isNotEmpty()) form else form.copy(tags = removeLastTag(form.tags)) }
    }

    fun removeTag(tag: String) {
        updateForm { form -> form.copy(tags = form.tags - tag) }
    }

    fun updateMemo(memo: String) {
        updateForm { form -> form.copy(memo = memo) }
    }

    fun consumeError() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    fun consumeToast() {
        _uiState.update { state -> state.copy(toast = null) }
    }

    fun consumeSavedPlace() {
        _uiState.update { state -> state.copy(savedPlace = null) }
    }

    fun consumeDeleteError() {
        _uiState.update { state -> state.copy(deleteErrorMessage = null) }
    }

    fun consumeDeletedPlace() {
        _uiState.update { state -> state.copy(deletedPlaceId = null) }
    }

    /** 메모·태그를 보낸다. 성공하면 화면을 닫고 장소 페이지에 토스트를 띄운다. */
    fun save() {
        if (!_uiState.value.canSave) return

        // 스페이스 없이 입력만 해 둔 태그도 확정한다. 화면의 칩과 보내는 값이 어긋나지 않게 상태부터.
        updateTagInput(_uiState.value.form?.tagInput.orEmpty() + " ")
        val form = _uiState.value.form ?: return

        // 코루틴 시작을 기다리지 않고 잠근다. 그 사이 들어온 두 번째 탭이 가드를 통과하지 않게.
        _uiState.update { state -> state.copy(saving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val updated = repository.updatePlace(
                    placeId = form.original.id,
                    memo = form.memo.trim(),
                    tags = form.tags,
                )
                _uiState.update { state ->
                    state.copy(form = null, saving = false, savedPlace = updated, toast = PLACE_UPDATED_TOAST)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "장소 수정 실패 (placeId=${form.original.id})", e)
                // 입력값은 그대로 둔다. 다시 누르면 된다.
                _uiState.update { state ->
                    state.copy(saving = false, errorMessage = e.toUserMessage(SAVE_FAILED_MESSAGE))
                }
            }
        }
    }

    fun delete(placeId: Long) {
        if (_uiState.value.deleting) return
        _uiState.update { state -> state.copy(deleting = true, deleteErrorMessage = null) }

        viewModelScope.launch {
            try {
                repository.deletePlace(placeId)
                _uiState.update { state -> state.copy(deleting = false, deletedPlaceId = placeId) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "장소 삭제 실패 (placeId=$placeId)", e)
                _uiState.update { state ->
                    state.copy(deleting = false, deleteErrorMessage = e.toUserMessage(DELETE_FAILED_MESSAGE))
                }
            }
        }
    }

    private fun updateForm(transform: (PlaceEditForm) -> PlaceEditForm) {
        _uiState.update { state -> state.form?.let { form -> state.copy(form = transform(form)) } ?: state }
    }
}
