package com.moamap.app.feature.mapdetail.presentation.personal

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.network.ApiException
import com.moamap.app.feature.mapdetail.domain.repository.PersonalMapNotFoundException
import com.moamap.app.feature.mapdetail.domain.repository.PersonalMapRepository
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

private const val TAG = "PersonalMapAddViewModel"

internal const val PERSONAL_MAP_ADDED_MESSAGE = "나만의 지도에 추가했어요"
internal const val PERSONAL_MAP_DUPLICATE_MESSAGE = "이미 나만의 지도에 있는 장소예요"
internal const val PERSONAL_MAP_NOT_FOUND_MESSAGE = "나만의 지도를 찾지 못했어요"
internal const val PERSONAL_MAP_ADD_FAILED_MESSAGE = "나만의 지도에 추가하지 못했어요"

/** `[409] PLACE_010: 해당 지도에 이미 등록된 장소입니다.` */
private const val DUPLICATE_PLACE_CODE = "PLACE_010"

private fun Throwable.toAddMessage(): String = when {
    this is ApiException && code == DUPLICATE_PLACE_CODE -> PERSONAL_MAP_DUPLICATE_MESSAGE
    this is PersonalMapNotFoundException -> PERSONAL_MAP_NOT_FOUND_MESSAGE
    else -> toUserMessage(PERSONAL_MAP_ADD_FAILED_MESSAGE)
}

/**
 * 장소 상세 시트의 「나만의 지도에 추가」 상태.
 *
 * 결과 안내는 시트 안에 띄운다. 시트 위로는 화면의 스낵바가 보이지 않는다.
 * [placeId] 로 어느 장소의 결과인지 가린다 - 다른 장소를 열었는데 앞 장소의 안내가 남으면 안 된다.
 */
@Immutable
data class PersonalMapAddUiState(
    val placeId: Long? = null,
    val adding: Boolean = false,
    val message: String? = null,
    /** 안내가 실패를 알리는지. 색을 가른다. */
    val failed: Boolean = false,
)

@HiltViewModel
class PersonalMapAddViewModel @Inject constructor(
    private val repository: PersonalMapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalMapAddUiState())
    val uiState: StateFlow<PersonalMapAddUiState> = _uiState.asStateFlow()

    private var addJob: Job? = null

    /** 시트가 이 장소를 열었다. 같은 장소면 남아 있는 안내를 그대로 둔다. */
    fun open(placeId: Long) {
        if (_uiState.value.placeId == placeId) return

        addJob?.cancel()
        _uiState.value = PersonalMapAddUiState(placeId = placeId)
    }

    fun close() {
        addJob?.cancel()
        _uiState.value = PersonalMapAddUiState()
    }

    /** 누르는 동안에는 다시 받지 않는다. 같은 장소가 두 번 들어가면 두 번째는 중복으로 실패한다. */
    fun add() {
        val placeId = _uiState.value.placeId ?: return
        if (_uiState.value.adding) return

        _uiState.update { state -> state.copy(adding = true, message = null, failed = false) }
        addJob = viewModelScope.launch {
            val (message, failed) = try {
                repository.addPlace(placeId)
                PERSONAL_MAP_ADDED_MESSAGE to false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "나만의 지도 추가 실패 (placeId=$placeId)", e)
                e.toAddMessage() to true
            }
            _uiState.update { state ->
                if (state.placeId != placeId) state
                else state.copy(adding = false, message = message, failed = failed)
            }
        }
    }
}
