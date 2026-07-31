package com.example.moamap.feature.officialmap.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.feature.officialmap.domain.model.OfficialMap
import com.example.moamap.feature.officialmap.domain.repository.OfficialMapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OfficialMapsState {
    data object Loading : OfficialMapsState
    data class Success(val maps: List<OfficialMap>) : OfficialMapsState
    data class Error(val message: String) : OfficialMapsState
}

@HiltViewModel
class OfficialMapViewModel @Inject constructor(
    private val repository: OfficialMapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OfficialMapsState>(OfficialMapsState.Loading)
    val uiState: StateFlow<OfficialMapsState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    /**
     * 참여 요청이 나가 있는 지도.
     *
     * 요청 하나를 들고 있으면 안 된다. 그러면 지도 하나에 참여하는 동안 다른 지도의 참여
     * 버튼까지 함께 죽는다 - 눌러도 아무 일이 없고 안내도 없어 원인을 찾기 어렵다. 막아야
     * 하는 건 같은 카드를 연달아 누르는 것뿐이라 지도별로 센다.
     *
     * 잠그지 않는다. `viewModelScope` 는 `Dispatchers.Main.immediate` 라 여기 드나드는
     * 일이 모두 메인 스레드에서 일어난다.
     */
    private val joiningMapIds = mutableSetOf<Long>()

    private companion object {
        const val TAG = "OfficialMapViewModel"
        const val LOAD_FAILED_MESSAGE = "공식지도를 불러오지 못했어요"
    }

    /**
     * 카드의 참여 버튼.
     *
     * 성공하면 목록을 다시 읽어 버튼이 "참여중" 으로 바뀐다. 응답만 보고 화면 상태를 직접
     * 고치지 않는 건, 참여로 멤버 수도 함께 늘어 어차피 목록이 낡기 때문이다.
     *
     * 실패는 로그만 남긴다. 이 화면에는 안내를 띄울 자리가 없고, 버튼이 "참여하기" 인 채로
     * 남아 다시 누를 수 있다.
     */
    fun join(mapId: Long) {
        // add 가 false 면 이 지도의 요청이 이미 나가 있다는 뜻이다.
        if (!joiningMapIds.add(mapId)) return

        viewModelScope.launch {
            try {
                repository.joinMap(mapId)
                load(keepCurrent = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "공식지도 참여 실패 (mapId=$mapId)", e)
            } finally {
                // 실패해도 지운다. 남겨 두면 그 지도는 다시 참여할 수 없다.
                joiningMapIds.remove(mapId)
            }
        }
    }

    /**
     * 화면이 보일 때 목록을 읽는다. 첫 조회도 이 경로가 겸한다.
     *
     * `init` 에서 첫 조회를 하지 않는 이유는, 화면이 처음 뜰 때 이 함수도 함께 불려
     * 같은 요청이 두 번 나가기 때문이다.
     */
    fun refresh() = load(keepCurrent = _uiState.value is OfficialMapsState.Success)

    fun retry() = load()

    /**
     * @param keepCurrent true 면 보고 있던 목록을 지우지 않는다. 돌아올 때마다 목록이
     *  사라졌다 나타나면 화면이 깜빡인다.
     */
    private fun load(keepCurrent: Boolean = false) {
        loadJob?.cancel()
        if (!keepCurrent) {
            _uiState.value = OfficialMapsState.Loading
        }
        loadJob = viewModelScope.launch {
            try {
                val maps = repository.getOfficialMaps()
                _uiState.value = OfficialMapsState.Success(maps)
            } catch (e: CancellationException) {
                // 다음 조회가 이미 시작됐다. 이 요청의 결과로 상태를 건드리면 안 된다.
                throw e
            } catch (e: Exception) {
                // 예외 메시지는 그대로 노출하지 않는다. ApiException 은 "[500] COMMON_005: ..."
                // 처럼 사용자에게 보여줄 수 없는 형태다.
                Log.w(TAG, "공식지도 목록 조회 실패", e)
                // 새로고침이 실패했는데 이미 보여줄 목록이 있으면 지우지 않는다.
                if (keepCurrent) return@launch
                _uiState.value = OfficialMapsState.Error(LOAD_FAILED_MESSAGE)
            }
        }
    }
}
